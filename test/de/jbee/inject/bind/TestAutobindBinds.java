package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Type.raw;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.Serializable;

import org.junit.Test;

import de.jbee.inject.Injector;
import de.jbee.inject.bind.Bootstrap;
import de.jbee.inject.bind.PackageModule;

public class TestAutobindBinds {

	static class AutobindBindsModule
			extends PackageModule {

		@Override
		protected void configure() {
			autobind( Integer.class ).to( 42 );
		}

	}

	private final Injector injector = Bootstrap.injector( AutobindBindsModule.class );

	@Test
	public void thatTheAutoboundTypeItselfIsBound() {
		assertThat( injector.resolve( dependency( raw( Integer.class ) ) ), is( 42 ) );
	}

	@Test
	public void thatDirectSuperclassOfAutoboundTypeIsBound() {
		assertThat( injector.resolve( dependency( raw( Number.class ) ) ).intValue(), is( 42 ) );
	}

	@Test
	public void thatSuperinterfaceOfAutoboundTypeIsBound() {
		assertEquals( injector.resolve( dependency( raw( Serializable.class ) ) ), 42 );
	}

	//TODO compareable<Integer>
}
