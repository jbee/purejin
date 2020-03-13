package se.jbee.inject.bind;

import static org.junit.Assert.assertSame;

import java.util.logging.Logger;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BootstrapperBundle;
import se.jbee.inject.bootstrap.Supply;
import se.jbee.inject.container.Supplier;

/**
 * A test that demonstrates how to extend the DI so that e.g. a class gets its
 * class-specific- {@link Logger} injected. Have a look how
 * {@link Supply#LOGGER} is implemented.
 *
 * You can use {@link Factory}s for simpler cases or {@link Supplier}s when more
 * context information are needed to provide the instance.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestLoggerBinds {

	private static class LoggerBindsBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(Adapter.LOGGER);
			install(LoggerBindsModule.class);
		}

	}

	private static class LoggerBindsModule extends BinderModule {

		@Override
		protected void declare() {
			construct(Foo.class);
		}

	}

	private static class Foo {

		final Logger logger;

		@SuppressWarnings("unused")
		Foo(Logger logger) {
			this.logger = logger;
		}
	}

	@Test
	public void thatEachClassGetsTheLoggerWithItsCanonicalName() {
		Injector injector = Bootstrap.injector(LoggerBindsBundle.class);
		Foo foo = injector.resolve(Foo.class);
		assertSame(Logger.getLogger(Foo.class.getCanonicalName()), foo.logger);
	}
}
