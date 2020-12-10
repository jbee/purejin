package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.CoreFeature;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.Name.named;

class TestPrimitiveArrayBridgeBinds {

	private static class TestPrimitiveArrayBridgeBindsBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(CoreFeature.PRIMITIVE_ARRAYS);
			install(TestPrimitiveArrayBridgeBindsModule.class);
		}

	}

	private static class TestPrimitiveArrayBridgeBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind(int.class).to(1);
			bind(named("wrapper"), Integer.class).to(2);
			bind(named("some"), Integer.class).to(() -> 3);

			bind(long.class).to(4L);
			bind(named("wrapper"), Long.class).to(5L);
			bind(named("some"), Long.class).to(() -> 6L);

			bind(float.class).to(7f);
			bind(named("wrapper"), Float.class).to(8f);
			bind(named("some"), Float.class).to(() -> 9f);

			bind(double.class).to(10d);
			bind(named("wrapper"), Double.class).to(11d);
			bind(named("some"), Double.class).to(() -> 12d);

			bind(boolean.class).to(true);
			bind(named("wrapper"), Boolean.class).to(true);
			bind(named("some"), Boolean.class).to(() -> true);
		}

	}

	private final Injector injector = Bootstrap.injector(
			TestPrimitiveArrayBridgeBindsBundle.class);

	@Test
	void primitiveArrayBridgeProvidesPrimitveInts() {
		int[] ints = injector.resolve(int[].class);
		assertEquals(3, ints.length);
		Set<Integer> actual = new HashSet<>();
		for (int v : ints)
			actual.add(v);
		assertEquals(new HashSet<>(asList(1, 2, 3)), actual);
	}

	@Test
	void primitiveArrayBridgeProvidesPrimitveLongs() {
		long[] longs = injector.resolve(long[].class);
		assertEquals(3, longs.length);
		Set<Long> actual = new HashSet<>();
		for (long v : longs)
			actual.add(v);
		assertEquals(new HashSet<>(asList(4L, 5L, 6L)), actual);
	}

	@Test
	void primitiveArrayBridgeProvidesPrimitveFloats() {
		float[] floats = injector.resolve(float[].class);
		assertEquals(3, floats.length);
		Set<Float> actual = new HashSet<>();
		for (float v : floats)
			actual.add(v);
		assertEquals(new HashSet<>(asList(7f, 8f, 9f)), actual);
	}

	@Test
	void primitiveArrayBridgeProvidesPrimitveDoubles() {
		double[] doubles = injector.resolve(double[].class);
		assertEquals(3, doubles.length);
		Set<Double> actual = new HashSet<>();
		for (double v : doubles)
			actual.add(v);
		assertEquals(new HashSet<>(asList(10d, 11d, 12d)), actual);
	}

	@Test
	void primitiveArrayBridgeProvidesPrimitveBooleans() {
		boolean[] booleans = injector.resolve(boolean[].class);
		// this might be surprising expectation at first,
		// the reason only 1 value is returned is that resolving arrays
		// always just returns each instance once and all Boolean.TRUE instances
		// are the very same instance
		assertEquals(1, booleans.length);
		assertEquals(true, booleans[0]);
	}
}
