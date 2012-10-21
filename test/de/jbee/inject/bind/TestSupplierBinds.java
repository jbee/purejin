package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.jbee.inject.Dependency;
import de.jbee.inject.Injector;
import de.jbee.inject.Supplier;

public class TestSupplierBinds {

	public static class SupplierBindsModule
			extends BinderModule
			implements Supplier<String> {

		@Override
		protected void declare() {
			bind( String.class ).toSupplier( SupplierBindsModule.class );
		}

		@Override
		public String supply( Dependency<? super String> dependency, Injector injector ) {
			return "foobar";
		}

	}

	@Test
	public void test() {
		Injector injector = Bootstrap.injector( SupplierBindsModule.class );
		String value = injector.resolve( dependency( String.class ) );
		assertThat( value, is( "foobar" ) );
	}
}
