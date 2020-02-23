package se.jbee.inject.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static se.jbee.inject.Type.raw;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency.SupplyFailed;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BootstrapperBundle;
import se.jbee.inject.bootstrap.Supply;

/**
 * This test show how to bring back custom behaviour before or after
 * service/action method calls. This had been in the core but as it is easy to
 * add one can build something like {@link ServiceInterceptor} specialized to
 * ones needs.
 */
public class TestServiceInvocationBinds {

	private static class ServiceInvocationBindsModule extends ActionModule {

		@Override
		protected void declare() {
			bindActionsIn(ServiceInvocationBindsService.class);
			plug(AssertInvocation.class).into(ServiceInterceptor.class);
			bind(Executor.class).to(InstrumentedExecutor.class);
		}

	}

	private static class ServiceInvocationBindsBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(ServiceInvocationBindsModule.class);
		}

	}

	@SuppressWarnings("unused")
	private static class ServiceInvocationBindsService {

		public int hashCode(String s) {
			return s.hashCode();
		}

		public void fail(String text) {
			throw new IllegalStateException(text);
		}
	}

	private static class AssertInvocation implements ServiceInterceptor<Long> {

		int afterCount;
		int beforeCount;
		int afterExceptionCount;

		@Override
		public <P, R> void after(Type<P> parameter, P value, Type<R> resultType,
				R result, Long before) {
			afterCount++;
			assertEquals("before state passed to after", 42L,
					before.longValue());
			assertEquals(result, "Foo".hashCode());
		}

		@Override
		public <P, R> Long before(Type<P> parameter, P value, Type<R> result) {
			beforeCount++;
			assertTrue("right type passed to before",
					parameter.type().equalTo(raw(String.class)));
			assertEquals("right value passed to before", "Foo", value);
			return 42L;
		}

		@Override
		public <P, R> void afterException(Type<P> parameter, P value,
				Type<R> result, Exception e, Long before) {
			afterExceptionCount++;
			assertTrue(e instanceof IllegalStateException);
		}

	}

	private final Injector injector = Bootstrap.injector(
			ServiceInvocationBindsBundle.class);
	private final AssertInvocation inv = injector.resolve(
			AssertInvocation.class);

	@Test
	public void thatInvocationIsInvokedBeforeAndAfterTheServiceMethodCall() {
		@SuppressWarnings("unchecked")
		Action<String, Integer> hashCode = injector.resolve(
				raw(Action.class).parametized(String.class, Integer.class));
		int beforeCount = inv.beforeCount;
		int afterCount = inv.afterCount;
		assertEquals("Foo".hashCode(), hashCode.run("Foo").intValue());
		assertEquals(beforeCount + 1, inv.beforeCount);
		assertEquals(afterCount + 1, inv.afterCount);
	}

	@Test
	public void thatInvocationIsInvokedAfterExceptionInTheServiceMethodCall() {
		@SuppressWarnings("unchecked")
		Action<String, Void> fail = injector.resolve(
				raw(Action.class).parametized(String.class, Void.class));
		int afterExceptionCount = inv.afterExceptionCount;
		try {
			fail.run("Foo");
		} catch (ActionExecutionFailed e) {
			assertTrue(e.getCause() instanceof IllegalStateException);
			assertEquals("Foo", e.getCause().getMessage());
			assertEquals(afterExceptionCount + 1, inv.afterExceptionCount);
			return;
		}
		fail("Exception expected");
	}

	@Test
	public void thatInvocationHandlersCanBeResolvedDirectly() {
		ServiceInterceptor<?>[] invocations = injector.resolve(
				ServiceInterceptor[].class);
		assertEquals(1, invocations.length);
		assertEquals(invocations[0].getClass(), AssertInvocation.class);
	}

	static final class InstrumentedExecutor implements Executor {

		private final ServiceInterceptor<?>[] invocations;

		InstrumentedExecutor(ServiceInterceptor<?>[] invocations) {
			this.invocations = invocations;
		}

		@Override
		public <I, O> O run(ActionSite<I, O> site, Object[] args, I value)
				throws ActionExecutionFailed {
			Object[] state = before(site.output, site.input, value);
			O res;
			try {
				res = site.output.rawType.cast(
						Supply.produce(site.action, site.owner, args));
			} catch (SupplyFailed e) {
				Exception ex = e;
				if (e.getCause() instanceof Exception) {
					ex = (Exception) e.getCause();
				}
				afterException(site.output, site.input, value, ex, state);
				throw new ActionExecutionFailed(
						site.action.getDeclaringClass().getSimpleName() + "#"
							+ site.action.getName() + " failed: "
							+ ex.getMessage(),
						ex);
			}
			after(site.output, site.input, value, res, state);
			return res;
		}

		@SuppressWarnings("unchecked")
		private <T, I, O> void afterException(Type<O> output, Type<I> input,
				I value, Exception e, Object[] states) {
			if (invocations.length == 0) {
				return;
			}
			for (int i = 0; i < invocations.length; i++) {
				try {
					ServiceInterceptor<T> inv = (ServiceInterceptor<T>) invocations[i];
					inv.afterException(input, value, output, e, (T) states[i]);
				} catch (RuntimeException re) {
					// warn that invocation before had thrown an exception
				}
			}
		}

		private <I, O> Object[] before(Type<O> output, Type<I> input, I value) {
			if (invocations.length == 0) {
				return null;
			}
			Object[] invokeState = new Object[invocations.length];
			for (int i = 0; i < invocations.length; i++) {
				try {
					invokeState[i] = invocations[i].before(input, value,
							output);
				} catch (RuntimeException e) {
					// warn that invocation before had thrown an exception
				}
			}
			return invokeState;
		}

		@SuppressWarnings("unchecked")
		private <T, I, O> void after(Type<O> output, Type<I> input, I value,
				O res, Object[] states) {
			if (invocations.length == 0) {
				return;
			}
			for (int i = 0; i < invocations.length; i++) {
				try {
					ServiceInterceptor<T> inv = (ServiceInterceptor<T>) invocations[i];
					inv.after(input, value, output, res, (T) states[i]);
				} catch (RuntimeException e) {
					// warn that invocation before had thrown an exception
				}
			}
		}

	}
}
