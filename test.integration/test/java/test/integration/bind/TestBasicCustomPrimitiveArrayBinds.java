package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.Supplier;
import se.jbee.inject.bind.Bootstrapper;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.DefaultFeature;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static se.jbee.inject.Name.named;
import static se.jbee.lang.Type.raw;

/**
 * This test demonstrates how to add user defined primitive array support.
 * <p>
 * The default way however to enable primitive array support is to install
 * {@link DefaultFeature#PRIMITIVE_ARRAYS} which by default
 * is not installed as part of the {@link Bootstrapper#installDefaults()} as
 * it is shown in {@link TestBasicPrimitiveArrayBridgeBinds}.
 *
 * @see TestBasicPrimitiveArrayBridgeBinds
 */
class TestBasicCustomPrimitiveArrayBinds {

	private static class TestBasicCustomPrimitiveArrayBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind(Integer.class).to(7);
			bind(named("answer"), Integer.class).to(42);
			bind(Name.ANY, int[].class).toSupplier(new IntArraySupplier());
		}
	}

	/**
	 * A {@link Supplier} like this would be needed for each of the primitive
	 * types.
	 */
	private static class IntArraySupplier implements Supplier<int[]> {

		@Override
		public int[] supply(Dependency<? super int[]> dep, Injector context) {
			return copyToIntArray(context.resolve(dep.typed(raw(Integer[].class))));
		}

		private int[] copyToIntArray(Integer[] values) {
			int[] primitives = new int[values.length];
			for (int i = 0; i < primitives.length; i++) {
				primitives[i] = values[i];
			}
			return primitives;
		}
	}

	private final Injector context = Bootstrap.injector(
			TestBasicCustomPrimitiveArrayBindsModule.class);

	@Test
	void primitiveArrayIsAvailable() {
		assertArrayEquals(new int[] { 7, 42 }, context.resolve(int[].class));
	}
}
