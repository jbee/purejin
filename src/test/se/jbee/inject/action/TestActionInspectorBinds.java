package se.jbee.inject.action;

import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.action.ActionModule.actionDependency;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Inspect;
import se.jbee.inject.bootstrap.Inspector;
import se.jbee.inject.util.Resource;

/**
 * The tests illustrates how to change the way service methods are identified by
 * binding a custom {@link Inspector} for services using
 * {@link ActionModule#discoverActionsBy(Inspector)}.
 */
public class TestActionInspectorBinds {

	private static class TestServiceInspectorModule extends ActionModule {

		@Override
		protected void declare() {
			discoverActionsBy(
					Inspect.all().methods().annotatedWith(Resource.class));
			bindActionsIn(ActionInspectorBindsActions.class);
		}

	}

	static class ActionInspectorBindsActions {

		int notBoundSinceNotAnnotated() {
			return 13;
		}

		@Resource
		int anActionYieldingInts() {
			return 42;
		}
	}

	private final Injector injector = Bootstrap.injector(
			TestServiceInspectorModule.class);

	@Test
	public void actionInspectorCanBeCustomized() {
		Action<Void, Integer> answer = injector.resolve(
				actionDependency(raw(Void.class), raw(Integer.class)));
		assertEquals(42, answer.exec(null).intValue());
	}
}
