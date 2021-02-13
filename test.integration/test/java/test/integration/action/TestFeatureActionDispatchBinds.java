package test.integration.action;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.action.Action;
import se.jbee.inject.action.ActionModule;
import se.jbee.inject.action.ActionSite;
import se.jbee.inject.action.ActionDispatch;
import se.jbee.inject.bootstrap.Bootstrap;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.action.Action.actionTypeOf;
import static se.jbee.inject.config.ProducesBy.declaredMethods;

/**
 * This is a double test. It tests the feature of setting your own {@link
 * ActionDispatch} as well as if the list of {@link
 * se.jbee.inject.action.ActionSite}s provided to the {@link
 * ActionDispatch} is cached by the core as long as no
 * change did occur. That means no suitable {@link java.lang.reflect.Method}
 * connected or disconnected.
 */
class TestFeatureActionDispatchBinds {

	private static class TestFeatureActionDispatchBindsModule extends
			ActionModule {

		@Override
		protected void declare() {
			bind(ActionDispatch.class).to(RecordingActionDispatch.class);

			connect(declaredMethods(false)).inAny(Bean.class).asAction();
			construct(Bean.class);
		}
	}

	public static class RecordingActionDispatch<A> implements
			ActionDispatch<A, A> {

		private final List<List<ActionSite<A, A>>> recorded = new ArrayList<>();

		@Override
		public A execute(A input, List<ActionSite<A, A>> sites) {
			recorded.add(sites);
			return input;
		}
	}

	public static class Bean {

		String lastParam;

		public String action(String param) {
			lastParam = param;
			return param; // this should never be called as the strategy never executes using any site
		}
	}

	private final Injector context = Bootstrap.injector(
			TestFeatureActionDispatchBindsModule.class);

	@Test
	void listOfActionSitesIsCachedWhenNotChanged() {
		Action<String, String> frontend = context.resolve(
				actionTypeOf(String.class, String.class));

		Bean backend = context.resolve(Bean.class); // connects the backend
		assertNull(backend.lastParam);

		RecordingActionDispatch<?> strategy = context.resolve(
				RecordingActionDispatch.class);
		assertEquals(0, strategy.recorded.size());

		frontend.run("Hello!");
		assertEquals(1, strategy.recorded.size());
		frontend.run("Hello, again!");
		assertEquals(2, strategy.recorded.size());
		assertSame(strategy.recorded.get(0), strategy.recorded.get(1));
	}
}
