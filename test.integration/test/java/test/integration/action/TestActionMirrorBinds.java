package test.integration.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.lang.Type.raw;
import static se.jbee.inject.action.ActionModule.actionDependency;
import static se.jbee.inject.config.ProducesBy.declaredMethods;

import org.junit.jupiter.api.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.action.Action;
import se.jbee.inject.action.ActionModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.ProducesBy;
import test.integration.util.Resource;

/**
 * The tests illustrates how to change the way service methods are identified by
 * binding a custom {@link ProducesBy} for services using
 * {@link ActionModule#discoverActionsBy(ProducesBy)}.
 */
public class TestActionMirrorBinds {

	private static class TestServiceMirrorModule extends ActionModule {

		@Override
		protected void declare() {
			discoverActionsBy(declaredMethods.annotatedWith(Resource.class));
			bindActionsIn(ActionMirrorBindsActions.class);
		}

	}

	public static class ActionMirrorBindsActions {

		public int notBoundSinceNotAnnotated() {
			return 13;
		}

		@Resource
		public int anActionYieldingInts() {
			return 42;
		}
	}

	private final Injector injector = Bootstrap.injector(
			TestServiceMirrorModule.class);

	@Test
	public void actionMirrorCanBeCustomized() {
		Action<Void, Integer> answer = injector.resolve(
				actionDependency(raw(Void.class), raw(Integer.class)));
		assertEquals(42, answer.run(null).intValue());
	}
}
