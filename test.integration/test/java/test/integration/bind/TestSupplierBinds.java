package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Supplier;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestSupplierBinds {

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
	void test() {
		Injector injector = Bootstrap.injector(SupplierBindsModule.class);
		String value = injector.resolve(String.class);
		assertEquals("foobar", value);
	}
}
