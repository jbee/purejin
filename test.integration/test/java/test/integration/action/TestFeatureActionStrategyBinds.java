package test.integration.action;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.action.Action;
import se.jbee.inject.action.ActionModule;
import se.jbee.inject.action.ActionSite;
import se.jbee.inject.action.ActionStrategy;
import se.jbee.inject.bootstrap.Bootstrap;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.action.Action.actionTypeOf;
import static se.jbee.inject.config.ProducesBy.declaredMethods;

/**
 * This is a double test. It tests the feature of setting your own {@link
 * se.jbee.inject.action.ActionStrategy} as well as if the list of {@link
 * se.jbee.inject.action.ActionSite}s provided to the {@link
 * se.jbee.inject.action.ActionStrategy} is cached by the core as long as no
 * change did occur. That means no suitable {@link java.lang.reflect.Method}
 * connected or disconnected.
 */
class TestFeatureActionStrategyBinds {

	private static class TestFeatureActionStrategyBindsModule extends
			ActionModule {

		@Override
		protected void declare() {
			bind(ActionStrategy.class).to(RecordingStrategy.class);

			connect(declaredMethods(false)).inAny(Bean.class).asAction();
			construct(Bean.class);
		}
	}

	public static class RecordingStrategy<A> implements ActionStrategy<A, A> {

		private List<List<ActionSite<A, A>>> recorded = new ArrayList<>();

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
			TestFeatureActionStrategyBindsModule.class);

	@Test
	void listOfActionSitesIsCachedWhenNotChanged() {
		Action<String, String> frontend = context.resolve(
				actionTypeOf(String.class, String.class));

		Bean backend = context.resolve(Bean.class); // connects the backend
		assertNull(backend.lastParam);

		RecordingStrategy strategy = context.resolve(RecordingStrategy.class);
		assertEquals(0, strategy.recorded.size());

		frontend.run("Hello!");
		assertEquals(1, strategy.recorded.size());
		frontend.run("Hello, again!");
		assertEquals(2, strategy.recorded.size());
		assertSame(strategy.recorded.get(0), strategy.recorded.get(1));
	}
}
