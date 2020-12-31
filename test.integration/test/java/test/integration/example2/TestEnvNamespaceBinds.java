package test.integration.example2;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.binder.Binder;
import test.Example;
import test.example2.Bus;
import test.example2.Car;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test example shows how {@link se.jbee.inject.binder.LocalEnvModule} can
 * be used to set strategies like {@link se.jbee.inject.config.ProducesBy} and
 * {@link se.jbee.inject.config.NamesBy} for a {@link Package} and their
 * subpackages.
 * <p>
 * In this example there is a {@link Car} and a {@link Bus} related package, one
 * picking up {@link Car}s when using {@link Binder.ScopedBinder#autobind()},
 * the other picking up {@link Bus}ses. Within the {@link Car} package is a
 * subpackages with a {@link Car[]} pool override. Last but not least this pool
 * behaviour is still set in the sub-package of the corporate {@link Car[]}
 * pool.
 * <p>
 * On the concept level this example verifies the general principle of local
 * name-spaces in the {@link se.jbee.inject.Env} and that the base class {@link
 * se.jbee.inject.binder.LocalEnvModule} affects the made bindings accordingly.
 * <p>
 * The reason this example only uses {@link Binder.ScopedBinder#autobind()} is
 * that is also should test the {@link se.jbee.inject.config.ProducesBy} and
 * {@link se.jbee.inject.config.NamesBy} strategies are resolved in the
 * localised name-space.
 */
class TestEnvNamespaceBinds {

	private final Injector context = Example.EXAMPLE_2.injector();

	@Test
	void inCarPackageCarProvidersAreBound() {
		assertCarExists("getPetersCar", "Peter");
		assertCarExists("getPaulsCar", "Paul");
	}

	@Test
	void inCarPackageBusProvidersAreNotBound() {
		assertBusDoesNotExist("busStopAtCarPark");
	}

	@Test
	void inBusPackageBusProvidersAreBound() {
		assertBusExists("toLondon", "London");
		assertBusExists("toGlasgow", "Glasgow");
	}

	@Test
	void inBusPackageCarProvidersAreNotBound() {
		assertCarDoesNotExist("carOfBusDriver");
	}

	@Test
	void inCarPoolPackageCarArrayProvidersAreBound() {
		Car[] pool = context.resolve("poolCars", Car[].class);
		assertEquals(2, pool.length);
		assertEquals("pool1", pool[0].owner);
		assertEquals("pool2", pool[1].owner);
	}

	@Test
	void inCorporateCarPoolPackageCarArrayProvidersAreBound() {
		Car[] pool = context.resolve("corporatePool", Car[].class);
		assertEquals(1, pool.length);
		assertEquals("monopoly1", pool[0].owner);
	}

	@Test
	void inCarPoolPackageCarProvidersAreNotBound() {
		assertCarDoesNotExist("nextPoolCar");
	}

	@Test
	void inCarPoolPackageBusProvidersAreNotBound() {
		assertBusDoesNotExist("stopAtCarPool");
	}

	private void assertBusDoesNotExist(String name) {
		assertThrows(UnresolvableDependency.ResourceResolutionFailed.class,
				() -> context.resolve(name, Bus.class));
	}

	private void assertCarDoesNotExist(String name) {
		assertThrows(UnresolvableDependency.ResourceResolutionFailed.class,
				() -> context.resolve(name, Car.class));
	}

	private void assertBusExists(String name, String to) {
		Bus car = context.resolve(name, Bus.class);
		assertNotNull(car);
		assertEquals(to, car.to);
	}

	private void assertCarExists(String name, String owner) {
		Car car = context.resolve(name, Car.class);
		assertNotNull(car);
		assertEquals(owner, car.owner);
	}
}
