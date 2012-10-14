package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static org.junit.Assert.fail;

import org.junit.Test;

import de.jbee.inject.Injector;
import de.jbee.inject.DIRuntimeException.MoreFrequentExpiryException;
import de.jbee.inject.util.Scoped;

public class TestScopedBinds {

	private static class Foo {

		@SuppressWarnings ( "unused" )
		Foo( Bar bar ) {
		}
	}

	private static class Bar {
		// just to demo
	}

	private static class ScopedBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			per( Scoped.APPLICATION ).construct( Foo.class );
			per( Scoped.INJECTION ).construct( Bar.class );
		}
	}

	@Test ( expected = MoreFrequentExpiryException.class )
	public void thatInjectingAnInjectionScopedInstanceIntoAppScopedInstanceThrowsAnException() {
		Injector injector = Bootstrap.injector( ScopedBindsModule.class );
		Foo foo = injector.resolve( dependency( Foo.class ) );
		fail( "It should not be possible to create a foo but got one: " + foo );
	}
}
