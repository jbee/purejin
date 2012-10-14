package de.jbee.inject.bind;

import org.junit.Test;

import de.jbee.inject.Dependency;
import de.jbee.inject.Injector;
import de.jbee.inject.DIRuntimeException.NoSuchResourceException;

public class TestInjectorExceptions {

	private static class TestInjectorBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			// we do no binds
		}

	}

	private final Injector injector = Bootstrap.injector( TestInjectorBundle.class );

	@Test ( expected = NoSuchResourceException.class )
	public void thatExceptionIsThrownWhenResolvingAnUnboundDependency() {
		injector.resolve( Dependency.dependency( String.class ) );
	}
}
