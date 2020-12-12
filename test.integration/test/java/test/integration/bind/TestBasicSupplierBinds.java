package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Supplier;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A very minimal test that shows how to use custom {@link Supplier}s.
 * <p>
 * A {@link Supplier} is the main "backend" (internal) abstraction to generate
 * or yield instances of the type supplied by the {@link Supplier}. Their
 * user-facing "frontend" counterpart is the {@link se.jbee.inject.Generator}.
 * <p>
 * While {@link Supplier}s stay inaccessible for the user of the {@link
 * Injector} context the {@link se.jbee.inject.Generator}s that act as an facade
 * for their internal {@link Supplier} can be resolved by users like any other
 * dependency.
 *
 * @see TestFeatureResolveResourceBinds
 */
class TestBasicSupplierBinds {

	public static class TestBasicSupplierBindsModule extends BinderModule
			implements Supplier<String> {

		@Override
		protected void declare() {
			bind(String.class).toSupplier(TestBasicSupplierBindsModule.class);
			bind(Integer.class).toSupplier((dep, context) -> 42);
		}

		@Override
		public String supply(Dependency<? super String> dep, Injector context) {
			return "foobar";
		}
	}

	private final Injector context = Bootstrap.injector(
			TestBasicSupplierBindsModule.class);

	@Test
	void supplierFromClassReference() {
		assertEquals("foobar", context.resolve(String.class));
	}

	@Test
	void supplierFromLambda() {
		assertEquals(42, context.resolve(Integer.class));
	}
}
