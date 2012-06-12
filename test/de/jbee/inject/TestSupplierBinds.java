package de.jbee.inject;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.jbee.inject.util.PackageModule;

public class TestSupplierBinds {

	public static class SupplierBindsModule
			extends PackageModule
			implements Supplier<String> {

		@Override
		protected void configure() {
			bind( String.class ).toSupplier( SupplierBindsModule.class );
		}

		@Override
		public String supply( Dependency<? super String> dependency, DependencyResolver context ) {
			return "foobar";
		}

	}

	@Test
	public void test() {
		Injector injector = Silk.injector( SupplierBindsModule.class );
		String value = injector.resolve( Dependency.dependency( Type.raw( String.class ) ) );
		assertThat( value, is( "foobar" ) );
	}
}
