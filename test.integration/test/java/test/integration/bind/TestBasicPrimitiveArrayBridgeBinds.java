package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.Installs;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.DefaultFeature;
import se.jbee.inject.defaults.DefaultFeatures;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.Name.named;

/**
 * The test demonstrates the {@link DefaultFeature#PRIMITIVE_ARRAYS} that installs
 * a bridge between wrapper arrays and their primitive counterparts so that
 * values of wrappers (and their 1-dimensional array types) can be resolved as
 * primitive arrays as well.
 *
 * @see TestBasicCustomPrimitiveArrayBinds
 */
class TestBasicPrimitiveArrayBridgeBinds {

	@Installs(features = DefaultFeature.class, by = DefaultFeatures.class)
	@DefaultFeatures(DefaultFeature.PRIMITIVE_ARRAYS)
	private static class TestBasicCustomPrimitiveArrayBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind(int.class).to(1);
			bind(named("wrapper"), Integer.class).to(2);
			bind(named("some"), Integer.class).toProvider(() -> 3);

			bind(long.class).to(4L);
			bind(named("wrapper"), Long.class).to(5L);
			bind(named("some"), Long.class).toProvider(() -> 6L);

			bind(float.class).to(7f);
			bind(named("wrapper"), Float.class).to(8f);
			bind(named("some"), Float.class).toProvider(() -> 9f);

			bind(double.class).to(10d);
			bind(named("wrapper"), Double.class).to(11d);
			bind(named("some"), Double.class).toProvider(() -> 12d);

			bind(boolean.class).to(true);
			bind(named("wrapper"), Boolean.class).to(true);
			bind(named("some"), Boolean.class).toProvider(() -> true);
		}
	}

	private final Injector context = Bootstrap.injector(
			TestBasicCustomPrimitiveArrayBindsModule.class);

	@Test
	void primitiveArrayBridgeProvidesPrimitiveIntArray() {
		int[] ints = context.resolve(int[].class);
		assertEquals(3, ints.length);
		Set<Integer> actual = new HashSet<>();
		for (int v : ints)
			actual.add(v);
		assertEquals(new HashSet<>(asList(1, 2, 3)), actual);
	}

	@Test
	void primitiveArrayBridgeProvidesPrimitiveLongArray() {
		long[] longs = context.resolve(long[].class);
		assertEquals(3, longs.length);
		Set<Long> actual = new HashSet<>();
		for (long v : longs)
			actual.add(v);
		assertEquals(new HashSet<>(asList(4L, 5L, 6L)), actual);
	}

	@Test
	void primitiveArrayBridgeProvidesPrimitiveFloatArray() {
		float[] floats = context.resolve(float[].class);
		assertEquals(3, floats.length);
		Set<Float> actual = new HashSet<>();
		for (float v : floats)
			actual.add(v);
		assertEquals(new HashSet<>(asList(7f, 8f, 9f)), actual);
	}

	@Test
	void primitiveArrayBridgeProvidesPrimitiveDoubleArray() {
		double[] doubles = context.resolve(double[].class);
		assertEquals(3, doubles.length);
		Set<Double> actual = new HashSet<>();
		for (double v : doubles)
			actual.add(v);
		assertEquals(new HashSet<>(asList(10d, 11d, 12d)), actual);
	}

	@Test
	void primitiveArrayBridgeProvidesPrimitiveBooleanArray() {
		boolean[] booleans = context.resolve(boolean[].class);
		// this might be surprising expectation at first,
		// the reason only 1 value is returned is that resolving arrays
		// always just returns each instance once and all Boolean.TRUE instances
		// are the very same instance
		// this is not a consequence of the wrapper => primitve conversion
		// but a consequence of the build in Injector behaviour that 1-d arrays
		// are resolved to all matching *unique* instances (if not bound otherwise)
		assertEquals(1, booleans.length);
		assertEquals(true, booleans[0]);
	}
}
