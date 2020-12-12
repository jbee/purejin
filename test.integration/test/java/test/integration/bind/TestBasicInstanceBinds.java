package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The test demonstrates binds that are 'linked' by type.
 */
class TestBasicInstanceBinds {

	public static class Foo {

	}

	private static class TestBasicInstanceBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(Number.class).to(Integer.class);
			bind(Integer.class).to(42);
			bind(Foo.class).to(Foo.class);
		}
	}

	private final Injector injector = Bootstrap.injector(
			TestBasicInstanceBindsModule.class);

	@Test
	void thatNumberDependencyIsResolvedToIntegerBoundSupplier() {
		Number number = injector.resolve(Number.class);
		assertTrue(number instanceof Integer);
		assertEquals(42, number.intValue());
	}

	@Test
	void thatTypeLinkedBackToItselfBecomesConstructorBinding() {
		assertNotNull(injector.resolve(Foo.class));
	}
}
