package test.integration.action;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.action.Action;
import se.jbee.inject.action.ActionModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.ProducesBy;
import test.integration.util.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.jbee.inject.action.Action.actionTypeOf;
import static se.jbee.inject.config.ProducesBy.declaredMethods;
import static se.jbee.inject.lang.Type.raw;

/**
 * The tests illustrates how to change the way service methods are identified by
 * binding a custom {@link ProducesBy} for services using
 */
class TestActionBasicBinds {

	private static class TestActionBasicBindsModule extends ActionModule {

		@Override
		protected void declare() {
			construct(Bean.class);
			connect(declaredMethods.annotatedWith(Resource.class)) //
					.in(Bean.class).asAction();
		}

	}

	public static class Bean {

		public int notBoundSinceNotAnnotated() {
			return 13;
		}

		@Resource
		public int anActionYieldingInts() {
			return 42;
		}
	}

	private final Injector injector = Bootstrap.injector(
			TestActionBasicBindsModule.class);

	@Test
	void actionMirrorCanBeCustomized() {
		Action<Void, Integer> answer = injector.resolve(
				actionTypeOf(raw(Void.class), raw(Integer.class)));
		assertNotNull(injector.resolve(Bean.class));
		assertEquals(42, answer.run(null).intValue());
	}
}
