package se.jbee.inject.action;

import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.action.ActionModule.actionDependency;
import static se.jbee.inject.config.ProductionMirror.allMethods;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.ProductionMirror;
import se.jbee.inject.util.Resource;

/**
 * The tests illustrates how to change the way service methods are identified by
 * binding a custom {@link ProductionMirror} for services using
 * {@link ActionModule#discoverActionsBy(ProductionMirror)}.
 */
public class TestActionMirrorBinds {

	private static class TestServiceMirrorModule extends ActionModule {

		@Override
		protected void declare() {
			discoverActionsBy(allMethods.annotatedWith(Resource.class));
			bindActionsIn(ActionMirrorBindsActions.class);
		}

	}

	static class ActionMirrorBindsActions {

		int notBoundSinceNotAnnotated() {
			return 13;
		}

		@Resource
		int anActionYieldingInts() {
			return 42;
		}
	}

	private final Injector injector = Bootstrap.injector(
			TestServiceMirrorModule.class);

	@Test
	public void actionMirrorCanBeCustomized() {
		Action<Void, Integer> answer = injector.resolve(
				actionDependency(raw(Void.class), raw(Integer.class)));
		assertEquals(42, answer.exec(null).intValue());
	}
}
