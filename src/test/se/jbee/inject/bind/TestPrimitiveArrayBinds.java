package se.jbee.inject.bind;

import static org.junit.Assert.assertArrayEquals;
import static se.jbee.inject.Name.named;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.container.Supplier;

/**
 * This test demonstrates how to add automatic primitive array support.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestPrimitiveArrayBinds {

	private static class PrimitiveArrayBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(Integer.class).to(7);
			bind(named("answer"), Integer.class).to(42);
			bind(int[].class).toSupplier(IntArraySupplier.class);
		}
	}

	/**
	 * A {@link Supplier} like this would be needed for each of the primitive
	 * types to make their arrays work like the wrapper arrays do. Since there
	 * is little benefit and easy to add in this it is not part of the tool
	 * Silk.
	 *
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	private static class IntArraySupplier implements Supplier<int[]> {

		@Override
		public int[] supply(Dependency<? super int[]> dep, Injector injector) {
			Integer[] values = injector.resolve(Integer[].class);
			int[] primitives = new int[values.length];
			for (int i = 0; i < primitives.length; i++) {
				primitives[i] = values[i];
			}
			return primitives;
		}

	}

	private final Injector injector = Bootstrap.injector(
			PrimitiveArrayBindsModule.class);

	@Test
	public void thatPrimitiveArrayIsAvailable() {
		assertArrayEquals(new int[] { 7, 42 }, injector.resolve(int[].class));
	}
}
