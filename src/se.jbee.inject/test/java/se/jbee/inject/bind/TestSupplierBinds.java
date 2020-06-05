package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.container.Supplier;

public class TestSupplierBinds {

	public static class SupplierBindsModule extends BinderModule
			implements Supplier<String> {

		@Override
		protected void declare() {
			bind(String.class).toSupplier(SupplierBindsModule.class);
		}

		@Override
		public String supply(Dependency<? super String> dep,
				Injector context) {
			return "foobar";
		}

	}

	@Test
	public void test() {
		Injector injector = Bootstrap.injector(SupplierBindsModule.class);
		String value = injector.resolve(String.class);
		assertEquals("foobar", value);
	}
}
