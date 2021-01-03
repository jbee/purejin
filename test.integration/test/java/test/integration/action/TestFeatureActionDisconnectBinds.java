package test.integration.action;

import org.junit.jupiter.api.Test;
import se.jbee.inject.DisconnectException;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency.NoMethodForDependency;
import se.jbee.inject.action.Action;
import se.jbee.inject.action.ActionModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static se.jbee.inject.action.Action.actionTypeOf;
import static se.jbee.inject.config.ProducesBy.declaredMethods;

/**
 * The test verifies the behaviour of {@link Action} when the underlying {@link
 * java.lang.reflect.Method}s want to be disconnected by throwing a {@link
 * DisconnectException}.
 * <p>
 * Note that disconnection is no feature of the action concept but of the
 * concept of {@link se.jbee.inject.config.Connector}s which is used as a basis
 * of connecting the abstraction {@link Action} to concrete {@link
 * java.lang.reflect.Method}s.
 */
class TestFeatureActionDisconnectBinds {

	private static class TestFeatureActionDisconnectBindsModule
			extends ActionModule {

		@Override
		protected void declare() {
			connect(declaredMethods(false)) //
					.inAny(Bean.class) //
					.asAction();

			construct(Bean.class);
		}
	}

	public static class Bean {

		int add1Calls;
		int add1UnstableCalls;
		int handleCalls;
		int handleUnstableCalls;

		public int add1(int val) {
			if (val > 4)
				throw new DisconnectException("give up");
			add1Calls++;
			return val + 1;
		}

		public int add1Unstable(int val) {
			add1UnstableCalls++;
			if (val <= 0)
				throw new DisconnectException("give up");
			return val + 1;
		}

		public void handle(String msg) {
			handleCalls++;
			if (msg.equals("fail-too"))
				throw new DisconnectException("give up");
		}

		public void handleUnstable(String msg) {
			handleUnstableCalls++;
			if (msg.equals("fail"))
				throw new DisconnectException("give up");
		}

	}

	private final Injector context = Bootstrap.injector(
			TestFeatureActionDisconnectBindsModule.class);

	@Test
	void roundRobinHasFailOverToOtherSiteWhenDisconnecting() {
		Action<Integer, Integer> math = context.resolve(
				actionTypeOf(int.class, int.class));

		Bean backend = context.resolve(Bean.class);

		// non fail-over calls
		assertEquals(2, math.run(1));
		assertEquals(2, math.run(1));
		// expect round robin
		assertEquals(1, backend.add1Calls);
		assertEquals(1, backend.add1UnstableCalls);
		// fail-over calls
		assertEquals(1, math.run(0));
		assertEquals(1, math.run(0));
		// because one of the above first went to the unstable site that
		// did a fail-over to the stable so the stable got called both times in the end
		assertEquals(3, backend.add1Calls);
		assertEquals(2, backend.add1UnstableCalls);
		// if we continue to call its always the stable one
		assertEquals(4, math.run(3));
		assertEquals(4, math.run(3));
		assertEquals(5, backend.add1Calls);
		assertEquals(2, backend.add1UnstableCalls);
		// now if we call the remaining site with a value > 4 this also disconnects that site
		// and our call should throw an exception
		assertThrows(NoMethodForDependency.class, () -> math.run(5));
	}

	@Test
	void multicastSkipsDisconnectingSiteUnlessItWasLast() {
		Action<String, Void> handler = context.resolve(
				actionTypeOf(String.class, void.class));

		Bean backend = context.resolve(Bean.class);

		// non-failing calls are multicast to all sites
		handler.run("Hello!");
		assertEquals(1, backend.handleCalls);
		assertEquals(1, backend.handleUnstableCalls);
		// now unstable should "silently" fail
		handler.run("fail");
		assertEquals(2, backend.handleCalls);
		assertEquals(2, backend.handleUnstableCalls);
		// now one should be out, all subsequent calls go to stable site only
		handler.run("Hello, again!");
		assertEquals(3, backend.handleCalls);
		assertEquals(2, backend.handleUnstableCalls);
		// now last one also fails
		assertThrows(NoMethodForDependency.class,
				() -> handler.run("fail-too"));
	}
}
