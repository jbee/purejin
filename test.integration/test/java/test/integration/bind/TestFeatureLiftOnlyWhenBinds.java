package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Lift;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Minimal test that shows how {@link Lift#onlyWhen(Predicate)} can be used
 * to add further conditions to a {@link Lift}.
 */
class TestFeatureLiftOnlyWhenBinds {

	private static class TestFeatureLiftOnlyWhenBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			Lift<Number> lift = (target, as, context) -> target.intValue() + 1;
			lift(Number.class).to(lift.onlyWhen(c -> c == Integer.class));

			//OBS! need to use toScoped so Lifts are applied to a constant
			bind(Integer.class).toScoped(2);
			bind(Long.class).toScoped(2L);
		}
	}

	private final Injector context = Bootstrap.injector(
			TestFeatureLiftOnlyWhenBindsModule.class);

	@Test
	void liftOnlyWhenFilterIsApplied() {
		assertEquals(3, context.resolve(Integer.class),
				"Integer did not get lifted as expected");
		assertEquals(2L, context.resolve(Long.class),
				"Long did get lifted which must mean the filter did not work");
	}
}
