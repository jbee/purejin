package test.integration.action;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.action.*;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.ProducesBy;
import se.jbee.lang.Type;

import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.lang.Type.raw;

/**
 * This test example explores how an abstraction similar to the interceptor
 * concept known from other dependency frameworks can be added to the {@link
 * Action} concept of this library.
 * <p>
 * In particular this example brings back a the notion of "before" and "after"
 * hooks as used in AOP.
 * <p>
 * The {@link Action} concept includes the {@link ActionExecutor} that is responsible
 * for actually executing the action call. This allows to implement more
 * advanced methods of execution, like thread pooling or locking contexts or as
 * in this test adding concepts like interceptors.
 */
class TestExampleServiceInterceptorBinds {

	/**
	 * Frames the invocation of {@link Action}s with further functionality that can
	 * be executed {@link #before(Type, Object, Type)} or {@link #after(Type,
	 * Object, Type, Object, Object)} the invoked {@linkplain Action}. Thereby a
	 * state can be passed between these tow methods. The result of first will be
	 * passed to the second as an argument. This allows them stay stateless.
	 * <p>
	 * A {@link ServiceInterceptor} intentionally doesn't give any control or access
	 * over/to the invoked {@linkplain Action} in order to be able to grant the same
	 * actual function invocation even with faulty {@linkplain ServiceInterceptor}s
	 * in place. That includes catching all exceptions thrown in {@link
	 * #before(Type, Object, Type)} or {@link #after(Type, Object, Type, Object,
	 * Object)}.
	 *
	 * @param <T> Type of the state transferred between {@link #before(Type, Object,
	 *            Type)} and {@link #after(Type, Object, Type, Object, Object)}.
	 */
	interface ServiceInterceptor<T> {

		<A, B> T before(Type<A> in, A value, Type<B> out);

		<A, B> void after(Type<A> in, A value, Type<B> out, B result, T before);

		<A, B> void afterException(Type<A> in, A value, Type<B> out, Exception ex,
				T before);

	}

	private static class ServiceInvocationBindsModule extends ActionModule {

		@Override
		protected void declare() {
			construct(TheService.class);
			connect(ProducesBy.OPTIMISTIC).inAny(TheService.class).asAction();
			// register the interceptor
			plug(AssertInvocationInterceptor.class) //
					.into(ServiceInterceptor.class);
			// replace the standard Executor with the instrumented one that runs interceptors
			bind(ActionExecutor.class).to(InstrumentingActionExecutor.class);
		}
	}

	/**
	 * The class implementing the {@link Action}s as methods
	 */
	public static class TheService {

		public int hashCode(String s) {
			return s.hashCode();
		}

		public void fail(String text) {
			throw new IllegalStateException(text);
		}
	}

	/**
	 * This {@link ServiceInterceptor} does not have any business logic.
	 * For this test it only verifies the call arguments and records the calls
	 * for later verification of the test scenario.
	 */
	public static class AssertInvocationInterceptor
			implements ServiceInterceptor<Long> {

		int afterCount;
		int beforeCount;
		int afterExceptionCount;

		@Override
		public <P, R> void after(Type<P> in, P value, Type<R> out, R result,
				Long before) {
			afterCount++;
			assertEquals(42L, before.longValue(),
					"before state passed to after");
			assertEquals(result, "Foo".hashCode());
		}

		@Override
		public <P, R> Long before(Type<P> in, P value, Type<R> out) {
			beforeCount++;
			assertTrue(in.type().equalTo(raw(String.class)),
					"right type passed to before");
			assertEquals("Foo", value, "right value passed to before");
			return 42L;
		}

		@Override
		public <P, R> void afterException(Type<P> in, P value, Type<R> out,
				Exception ex, Long before) {
			afterExceptionCount++;
			assertTrue(ex instanceof IllegalStateException,
					"was: " + ex.toString());
		}
	}

	private final Injector context = Bootstrap.injector(
			ServiceInvocationBindsModule.class);

	private final AssertInvocationInterceptor invocations = context.resolve(
			AssertInvocationInterceptor.class);

	@Test
	void beforeAndAfterAreInvokedOnSuccessfulServiceMethodCall() {
		@SuppressWarnings("unchecked") Action<String, Integer> hashCode = context.resolve(
				raw(Action.class).parameterized(String.class, Integer.class));
		assertNotNull(context.resolve(TheService.class));
		int beforeCount = invocations.beforeCount;
		int afterCount = invocations.afterCount;
		assertEquals("Foo".hashCode(), hashCode.run("Foo").intValue());
		assertEquals(beforeCount + 1, invocations.beforeCount);
		assertEquals(afterCount + 1, invocations.afterCount);
	}

	@Test
	void afterExceptionIsInvokedWhenServiceMethodThrewException() {
		@SuppressWarnings("unchecked")
		Action<String, Void> fail = context.resolve(
				raw(Action.class).parameterized(String.class, Void.class));
		assertNotNull(context.resolve(TheService.class));
		int afterExceptionCount = invocations.afterExceptionCount;
		Exception e = assertThrows(ActionExecutionFailed.class,
				() -> fail.run("Foo"));
		assertTrue(e.getCause() instanceof IllegalStateException);
		assertEquals("Foo", e.getCause().getMessage());
		assertEquals(afterExceptionCount + 1, invocations.afterExceptionCount);
	}

	@Test
	void interceptorsCanBeResolvedDirectly() {
		ServiceInterceptor<?>[] interceptors = context.resolve(
				ServiceInterceptor[].class);
		assertNotNull(context.resolve(TheService.class));
		assertEquals(1, interceptors.length);
		assertEquals(interceptors[0].getClass(), AssertInvocationInterceptor.class);
	}

	public static final class InstrumentingActionExecutor
			implements ActionExecutor {

		private final ServiceInterceptor<?>[] interceptors;

		public InstrumentingActionExecutor(ServiceInterceptor<?>[] interceptors) {
			this.interceptors = interceptors;
		}

		@Override
		public <A, B> B execute(ActionSite<A, B> site, Object[] args, A value)
				throws ActionExecutionFailed {
			if (interceptors.length == 0) {
				return site.call(args, null);
			}
			Object[] state = before(site.out, site.in, value);
			B res = site.call(args,
					ex -> afterException(site.out, site.in, value, ex, state));
			after(site.out, site.in, value, res, state);
			return res;
		}

		private <A, B> Object[] before(Type<B> out, Type<A> in, A value) {
			Object[] states = new Object[interceptors.length];
			runSafely(states,
					(interceptor, state) -> interceptor.before(in, value, out));
			return states;
		}

		private <T, A, B> void afterException(Type<B> out, Type<A> in, A value,
				Exception e, Object[] states) {
			runSafely(states, (interceptor, state) -> {
				interceptor.afterException(in, value, out, e, state);
				return state;
			});
		}

		private <T, A, B> void after(Type<B> out, Type<A> in, A value, B res,
				Object[] states) {
			runSafely(states, (interceptor, state) -> {
				interceptor.after(in, value, out, res, state);
				return state;
			});
		}

		@SuppressWarnings("unchecked")
		private <T> void runSafely(Object[] states,
				BiFunction<ServiceInterceptor<T>, T, T> operation) {
			for (int i = 0; i < interceptors.length; i++) {
				try {
					states[i] = operation.apply(
							(ServiceInterceptor<T>) interceptors[i],
							(T) states[i]);
				} catch (RuntimeException e) {
					// warn that interceptor operation had thrown an exception
				}
			}
		}
	}
}
