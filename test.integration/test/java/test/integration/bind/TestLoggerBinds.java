package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.CoreFeature;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A test that demonstrates how to extend the DI so that e.g. a class gets its
 * class-specific- {@link Logger} injected. Have a look how
 * {@link CoreFeature#LOGGER} is implemented.
 */
class TestLoggerBinds {

	private static class LoggerBindsBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(CoreFeature.LOGGER);
			install(LoggerBindsModule.class);
		}

	}

	private static class LoggerBindsModule extends BinderModule {

		@Override
		protected void declare() {
			construct(Foo.class);
		}

	}

	public static class Foo {

		final Logger logger;

		@SuppressWarnings("unused")
		public Foo(Logger logger) {
			this.logger = logger;
		}
	}

	@Test
	void thatEachClassGetsTheLoggerWithItsCanonicalName() {
		Injector injector = Bootstrap.injector(LoggerBindsBundle.class);
		Foo foo = injector.resolve(Foo.class);
		assertSame(Logger.getLogger(Foo.class.getCanonicalName()), foo.logger);
	}
}
