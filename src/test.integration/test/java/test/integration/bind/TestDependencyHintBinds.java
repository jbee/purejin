package test.integration.bind;

import org.junit.Test;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.CoreFeature;

import java.util.logging.Logger;

import static org.junit.Assert.assertSame;
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
 * @see TestConstructorHintBinds
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestDependencyHintBinds {

	private static class DependencyParameterBindsBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(CoreFeature.LOGGER);
			install(DependencyParameterBindsModule.class);
		}

	}

	private static class DependencyParameterBindsModule extends BinderModule {

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
	public void thatDependencyParameterIsUnderstood() {
		Injector resolver = Bootstrap.injector(
				DependencyParameterBindsBundle.class);
		Bean bean = resolver.resolve(Bean.class);
		Logger expected = Logger.getLogger(
				BinderModule.class.getCanonicalName());
		assertSame(expected.getName(), bean.logger.getName());
	}
}
