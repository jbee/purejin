package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.BuildUp;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.lang.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A test that demonstrates how the {@link BuildUp.Sequencer} can be used to
 * order {@link BuildUp}s.
 *
 * In this example the {@link BuildUp} are sorted in reverse alphabetically order
 * of their class names. One of them is a lambda which means the name starts
 * with the name of the surrounding {@link TestFeatureBuildUpSequencerBinds} class.
 */
class TestFeatureBuildUpSequencerBinds {

	public static final class Bean {

		List<String> names = new ArrayList<>();
	}

	static final class TestFeatureBuildUpSequencerBindsModule
			extends BinderModule implements BuildUp.Sequencer {

		@Override
		protected void declare() {
			bind(Bean.class).toConstructor();
			initbind(Bean.class).to(InitA.class);
			initbind(Bean.class).to(InitB.class);
			initbind(Bean.class).to((bean, as, context) -> {
				bean.names.add("c");
				return bean;
			});
			bind(BuildUp.Sequencer.class).to(this);
		}

		@Override
		public BuildUp<?>[] order(Class<?> actualType,
				BuildUp<?>[] set) {
			Arrays.sort(set, (a, b) -> b.getClass().getSimpleName().compareTo(
					a.getClass().getSimpleName()));
			return set;
		}

	}

	public static final class InitA implements BuildUp<Bean> {

		@Override
		public Bean buildUp(Bean bean, Type<?> as, Injector context) {
			bean.names.add("a");
			return bean;
		}

	}

	public static final class InitB implements BuildUp<Bean> {

		@Override
		public Bean buildUp(Bean bean, Type<?> as, Injector context) {
			bean.names.add("b");
			return bean;
		}

	}

	private final Injector injector = Bootstrap.injector(
			TestFeatureBuildUpSequencerBindsModule.class);

	@Test
	void orderOfBuildUpsIsAffectedBySequencer() {
		assertEquals(asList("c", "b", "a"), injector.resolve(Bean.class).names);
	}
}
