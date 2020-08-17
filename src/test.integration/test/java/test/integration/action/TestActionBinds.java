package test.integration.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static se.jbee.inject.lang.Type.raw;
import static se.jbee.inject.action.ActionModule.actionDependency;

import org.junit.jupiter.api.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.action.Action;
import se.jbee.inject.action.ActionExecutionFailed;
import se.jbee.inject.action.ActionModule;
import se.jbee.inject.bootstrap.Bootstrap;

public class TestActionBinds {

	public static class ActionBindsModule extends ActionModule {

		@Override
		protected void declare() {
			bindActionsIn(MyService.class);
			bindActionsIn(MyOtherService.class);
		}

	}

	public static class MyService {

		public Integer negate(Number value) {
			return -value.intValue();
		}

		public Void error() {
			throw new IllegalStateException("This should be wrapped!");
		}
	}

	public static class MyOtherService {

		public int mul2(int value, Action<Float, Integer> service) {
			return value * 2 + service.run(2.8f);
		}

		public int round(float value) {
			return Math.round(value);
		}
	}

	@Test
	public void actionsDecoupleConcreteMethods() {
		Injector injector = Bootstrap.injector(ActionBindsModule.class);
		Action<Integer, Integer> mul2 = injector.resolve(
				actionDependency(raw(Integer.class), raw(Integer.class)));
		assertNotNull(mul2);
		assertEquals(9, mul2.run(3).intValue());
		Action<Number, Integer> negate = injector.resolve(
				actionDependency(raw(Number.class), raw(Integer.class)));
		assertNotNull(mul2);
		assertEquals(-3, negate.run(3).intValue());
		assertEquals(11, mul2.run(4).intValue());
	}

	@Test
	public void exceptionsAreWrappedInActionMalfunction() {
		Injector injector = Bootstrap.injector(ActionBindsModule.class);
		Action<Void, Void> error = injector.resolve(
				actionDependency(raw(Void.class), raw(Void.class)));
		try {
			error.run(null);
			fail("Expected an exception...");
		} catch (ActionExecutionFailed e) {
			assertSame(IllegalStateException.class, e.getCause().getClass());
		}
	}
}
