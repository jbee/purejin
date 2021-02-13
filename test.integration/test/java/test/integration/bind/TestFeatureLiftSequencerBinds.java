package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Lift;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.lang.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A test that demonstrates how the {@link Lift.Sequencer} can be used to
 * order {@link Lift}s.
 *
 * In this example the {@link Lift} are sorted in reverse alphabetically order
 * of their class names. One of them is a lambda which means the name starts
 * with the name of the surrounding {@link TestFeatureLiftSequencerBinds} class.
 */
class TestFeatureLiftSequencerBinds {

	public static final class Bean {

		List<String> names = new ArrayList<>();
	}

	static final class TestFeatureLiftSequencerBindsModule
			extends BinderModule implements Lift.Sequencer {

		@Override
		protected void declare() {
			bind(Bean.class).toConstructor();
			lift(Bean.class).to(InitA.class);
			lift(Bean.class).to(InitB.class);
			lift(Bean.class).to((bean, as, context) -> {
				bean.names.add("c");
				return bean;
			});
			bind(Lift.Sequencer.class).to(this);
		}

		@Override
		public Lift<?>[] order(Class<?> actualType,
				Lift<?>[] set) {
			Arrays.sort(set, (a, b) -> b.getClass().getSimpleName().compareTo(
					a.getClass().getSimpleName()));
			return set;
		}

	}

	public static final class InitA implements Lift<Bean> {

		@Override
		public Bean lift(Bean bean, Type<?> as, Injector context) {
			bean.names.add("a");
			return bean;
		}

	}

	public static final class InitB implements Lift<Bean> {

		@Override
		public Bean lift(Bean bean, Type<?> as, Injector context) {
			bean.names.add("b");
			return bean;
		}

	}

	private final Injector injector = Bootstrap.injector(
			TestFeatureLiftSequencerBindsModule.class);

	@Test
	void orderOfLiftsIsAffectedBySequencer() {
		assertEquals(asList("c", "b", "a"), injector.resolve(Bean.class).names);
	}
}
