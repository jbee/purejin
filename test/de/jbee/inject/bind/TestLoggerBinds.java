package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.util.logging.Logger;

import org.junit.Test;

import de.jbee.inject.DependencyResolver;

public class TestLoggerBinds {

	private static class LoggerBindsBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install( BuildinBundle.LOGGER );
			install( LoggerBindsModule.class );
		}

	}

	private static class LoggerBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( Foo.class );
		}

	}

	private static class Foo {

		final Logger logger;

		@SuppressWarnings ( "unused" )
		Foo( Logger logger ) {
			this.logger = logger;
		}
	}

	@Test
	public void thatEachClassGetsTheLoggerWithItsCanonicalName() {
		DependencyResolver injector = Bootstrap.injector( LoggerBindsBundle.class );
		Foo foo = injector.resolve( dependency( Foo.class ) );
		assertThat( foo.logger, sameInstance( Logger.getLogger( Foo.class.getCanonicalName() ) ) );
	}
}
