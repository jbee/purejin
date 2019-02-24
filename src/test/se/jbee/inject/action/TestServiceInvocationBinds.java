package se.jbee.inject.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static se.jbee.inject.Type.raw;

import java.lang.reflect.Method;

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
 * add one can build something like {@link ServiceInvocation} specialized to
 * ones needs.
 *
 * @author jan
 */
public class TestServiceInvocationBinds {

	private static class ServiceInvocationBindsModule extends ActionModule {

		@Override
		protected void declare() {
			bindActionsIn(ServiceInvocationBindsService.class);
			plug(AssertInvocation.class).into(ServiceInvocation.class);
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

	private static class AssertInvocation implements ServiceInvocation<Long> {

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
			assertEquals("right value passed to before", value, "Foo");
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
		assertEquals("Foo".hashCode(), hashCode.exec("Foo").intValue());
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
			fail.exec("Foo");
		} catch (ActionMalfunction e) {
			assertTrue(e.getCause() instanceof IllegalStateException);
			assertEquals(e.getCause().getMessage(), "Foo");
			assertEquals(afterExceptionCount + 1, inv.afterExceptionCount);
			return;
		}
		fail("Exception expected");
	}

	@Test
	public void thatInvocationHandlersCanBeResolvedDirectly() {
		ServiceInvocation<?>[] invocations = injector.resolve(
				ServiceInvocation[].class);
		assertEquals(1, invocations.length);
		assertEquals(invocations[0].getClass(), AssertInvocation.class);
	}

	static final class InstrumentedExecutor implements Executor {

		private final ServiceInvocation<?>[] invocations;

		InstrumentedExecutor(ServiceInvocation<?>[] invocations) {
			this.invocations = invocations;
		}

		@Override
		public <I, O> O exec(Object impl, Method action, Object[] args,
				Type<O> output, Type<I> input, I value)
				throws ActionMalfunction {
			Object[] state = before(output, input, value);
			O res;
			try {
				res = output.rawType.cast(Supply.method(action, impl, args));
			} catch (SupplyFailed e) {
				Exception ex = e;
				if (e.getCause() instanceof Exception) {
					ex = (Exception) e.getCause();
				}
				afterException(output, input, value, ex, state);
				throw new ActionMalfunction(
						action.getDeclaringClass().getSimpleName() + "#"
							+ action.getName() + " failed: " + ex.getMessage(),
						ex);
			}
			after(output, input, value, res, state);
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
					ServiceInvocation<T> inv = (ServiceInvocation<T>) invocations[i];
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
					ServiceInvocation<T> inv = (ServiceInvocation<T>) invocations[i];
					inv.after(input, value, output, res, (T) states[i]);
				} catch (RuntimeException e) {
					// warn that invocation before had thrown an exception
				}
			}
		}

	}
}
