package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.Serializable;

import org.junit.Test;

import de.jbee.inject.DependencyResolver;

public class TestAutobindBinds {

	static class AutobindBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			autobind( Integer.class ).to( 42 );
		}

	}

	private final DependencyResolver injector = Bootstrap.injector( AutobindBindsModule.class );

	@Test
	public void thatTheAutoboundTypeItselfIsBound() {
		assertThat( injector.resolve( dependency( Integer.class ) ), is( 42 ) );
	}

	@Test
	public void thatDirectSuperclassOfAutoboundTypeIsBound() {
		assertThat( injector.resolve( dependency( Number.class ) ).intValue(), is( 42 ) );
	}

	@Test
	public void thatSuperinterfaceOfAutoboundTypeIsBound() {
		assertEquals( injector.resolve( dependency( Serializable.class ) ), 42 );
	}

	//TODO compareable<Integer>
}
