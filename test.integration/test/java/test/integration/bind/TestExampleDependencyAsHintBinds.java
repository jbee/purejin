package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.Installs;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.DefaultFeature;
import se.jbee.inject.defaults.DefaultFeatures;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertSame;
import static se.jbee.inject.Dependency.dependency;

/**
 * This test demonstrates the most powerful {@link se.jbee.inject.Hint}: a
 * {@link Dependency}.
 *
 * It allows to also describe what {@link Instance} should be used dependent on
 * its parent(s) it would be {@link Dependency#injectingInto(Class)}. Though
 * this we can tell to inject the {@link Logger} that would be injected into the
 * {@link BinderModule} class into our test object {@link Bean}.
 *
 * @see TestBasicHintsBinds
 */
class TestExampleDependencyAsHintBinds {

	@Installs(features = DefaultFeature.class, by = DefaultFeatures.class)
	@DefaultFeatures(DefaultFeature.LOGGER)
	private static class TestExampleDependencyAsHintBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind(Bean.class).toConstructor(
					dependency(Logger.class).injectingInto(BinderModule.class).asHint());
		}
	}

	public static class Bean {

		final Logger logger;

		@SuppressWarnings("unused")
		public Bean(Logger logger) {
			this.logger = logger;
		}
	}

	@Test
	void dependencyHintAffectsInjection() {
		Injector resolver = Bootstrap.injector(
				TestExampleDependencyAsHintBindsModule.class);
		Bean bean = resolver.resolve(Bean.class);
		Logger expected = Logger.getLogger(
				BinderModule.class.getCanonicalName());
		assertSame(expected.getName(), bean.logger.getName());
	}
}
