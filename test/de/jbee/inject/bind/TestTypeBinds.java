package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.jbee.inject.DependencyResolver;

public class TestTypeBinds {

	private static class TypeBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( Number.class ).to( Integer.class );
			bind( Integer.class ).to( 42 );
		}

	}

	@Test
	public void test() {
		DependencyResolver injector = Bootstrap.injector( TypeBindsModule.class );
		Number number = injector.resolve( dependency( Number.class ) );
		assertThat( number, instanceOf( Integer.class ) );
		assertThat( number.intValue(), is( 42 ) );
	}
}