package test.integration.action;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.action.*;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.lang.Type;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.config.ProducesBy.allMethods;
import static se.jbee.inject.lang.Type.raw;

/**
 * This test show how to bring back custom behaviour before or after
 * service/action method calls. This had been in the core but as it is easy to
 * add one can build something like {@link ServiceInterceptor} specialized to
 * ones needs.
 */
class TestServiceInvocationBinds {

	static class ServiceInvocationBindsModule extends ActionModule {

		@Override
		protected void declare() {
			construct(ServiceInvocationBindsService.class);
			connect(allMethods).in(ServiceInvocationBindsService.class).asAction();
			plug(AssertInvocation.class).into(ServiceInterceptor.class);
			bind(Executor.class).to(InstrumentedExecutor.class);
		}
	}

	public static class ServiceInvocationBindsService {

		public int hashCode(String s) {
			return s.hashCode();
		}

		public void fail(String text) {
			throw new IllegalStateException(text);
		}
	}

	public static class AssertInvocation implements ServiceInterceptor<Long> {

		int afterCount;
		int beforeCount;
		int afterExceptionCount;

		@Override
		public <P, R> void after(Type<P> parameter, P value, Type<R> resultType,
				R result, Long before) {
			afterCount++;
			assertEquals(42L,
					before.longValue(), "before state passed to after");
			assertEquals(result, "Foo".hashCode());
		}

		@Override
		public <P, R> Long before(Type<P> parameter, P value, Type<R> result) {
			beforeCount++;
			assertTrue(parameter.type().equalTo(raw(String.class)),
					"right type passed to before");
			assertEquals("Foo", value, "right value passed to before");
			return 42L;
		}

		@Override
		public <P, R> void afterException(Type<P> parameter, P value,
				Type<R> result, Exception e, Long before) {
			afterExceptionCount++;
			assertTrue(e instanceof IllegalStateException, "was: " + e.toString());
		}

	}

	private final Injector injector = Bootstrap.injector(
			ServiceInvocationBindsModule.class);
	private final AssertInvocation inv = injector.resolve(
			AssertInvocation.class);

	@Test
	void thatInvocationIsInvokedBeforeAndAfterTheServiceMethodCall() {
		@SuppressWarnings("unchecked") Action<String, Integer> hashCode = injector.resolve(
				raw(Action.class).parametized(String.class, Integer.class));
		assertNotNull(injector.resolve(ServiceInvocationBindsService.class));
		int beforeCount = inv.beforeCount;
		int afterCount = inv.afterCount;
		assertEquals("Foo".hashCode(), hashCode.run("Foo").intValue());
		assertEquals(beforeCount + 1, inv.beforeCount);
		assertEquals(afterCount + 1, inv.afterCount);
	}

	@Test
	void thatInvocationIsInvokedAfterExceptionInTheServiceMethodCall() {
		@SuppressWarnings("unchecked")
		Action<String, Void> fail = injector.resolve(
				raw(Action.class).parametized(String.class, Void.class));
		assertNotNull(injector.resolve(ServiceInvocationBindsService.class));
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
	void thatInvocationHandlersCanBeResolvedDirectly() {
		ServiceInterceptor<?>[] invocations = injector.resolve(
				ServiceInterceptor[].class);
		assertNotNull(injector.resolve(ServiceInvocationBindsService.class));
		assertEquals(1, invocations.length);
		assertEquals(invocations[0].getClass(), AssertInvocation.class);
	}

	public static final class InstrumentedExecutor implements Executor {

		private final ServiceInterceptor<?>[] invocations;

		public InstrumentedExecutor(ServiceInterceptor<?>[] invocations) {
			this.invocations = invocations;
		}

		@Override
		public <I, O> O run(ActionSite<I, O> site, Object[] args, I value)
				throws ActionExecutionFailed {
			Object[] state = before(site.out, site.in, value);
			O res = site.call(args,
					ex -> afterException(site.out, site.in, value, ex, state));
			after(site.out, site.in, value, res, state);
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
