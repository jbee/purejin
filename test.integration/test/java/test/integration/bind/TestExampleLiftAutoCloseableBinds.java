package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.Lift;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BinderModuleWith;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.lang.Type;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.lang.Type.raw;

/**
 * The tests demonstrates how the {@link Lift} and
 * {@link Binder#lift()} can be used to e.g. install a "shutdown hook" that
 * would automatically close all {@link AutoCloseable}s. Here the "shutdown
 * hook" of course is simulated so we can test for it being invoked. In a real
 * scenario one would use {@link Runtime#addShutdownHook(Thread)}.
 */
class TestExampleLiftAutoCloseableBinds {

	static final class TestExampleLiftAutoCloseableBindsModule
			extends BinderModule implements Lift<Injector> {

		@Override
		protected void declare() {
			lift().to(this);
			lift().to(AutoCloseableLift.class);
			construct(SingletonResource.class);
		}

		@Override
		public Injector lift(Injector target, Type<?> as, Injector context) {
			// just to show that one could use the module itself as well
			// this e.g. allows to pass down and use setup data by using
			// PresetModule's as shown with TestInitialiserBindsPresetModule
			moduleInitRan = true;
			return target;
		}

	}

	static final class TestExampleLiftAutoCloseableBindsModuleWith
			extends BinderModuleWith<Integer> implements Lift<Injector> {

		Integer setup;

		@Override
		protected void declare(Integer setup) {
			//... some binds
			lift().to(this);

			this.setup = setup;
		}

		@Override
		public Injector lift(Injector target, Type<?> as, Injector context) {
			assertNotNull(setup);
			// use setup to initialize something
			return target;
		}
	}

	public static class AutoCloseableLift implements Lift<Injector> {

		public AutoCloseableLift(AutoCloseable[] autoCloseable) {
			// since this instance is created by the container it is also properly injected.
			// so this can be used to receive instances that should be initialized as well
			// in this case we just did it to show the possibility but it is not needed
			assertEquals(0, autoCloseable.length);
			// however in this case there is no way to express the type
			// "? extends AutoCloseable[]" in the java language and
			// AutoCloseable[] is empty since we did not bind anything
			// explicitly or implicitly to the AutoCloseable interface
			// in such cases this must be done in the init method as shown below
		}

		@Override
		public Injector lift(Injector target, Type<?> as, Injector context) {
			// by the use of upper bound we receive all implementing classes
			// even though they have not be bound explicitly for AutoCloseable.
			AutoCloseable[] autoCloseable = target.resolve(
					raw(AutoCloseable[].class).asUpperBound());
			assertTrue(autoCloseable.length > 0);
			shutdownHookMock = () -> {
				for (AutoCloseable a : autoCloseable)
					try {
						a.close();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
			};
			return target;
		}

	}

	/**
	 * This class simulates some singleton that needs to be closed like a DB
	 * connection instance.
	 */
	public static class SingletonResource implements AutoCloseable {

		boolean isClosed = false;

		@Override
		public void close() {
			isClosed = true;
		}

	}

	static Runnable shutdownHookMock;
	static boolean moduleInitRan = false;

	@Test
	void liftCanBeUsedToCloseAnyAutoCloseable() {
		Injector injector = Bootstrap.injector(
				TestExampleLiftAutoCloseableBindsModule.class);

		assertNotNull(shutdownHookMock);
		@SuppressWarnings("resource")
		SingletonResource resource = injector.resolve(SingletonResource.class);
		assertNotNull(resource);
		assertFalse(resource.isClosed);
		shutdownHookMock.run(); // simulated shutdown
		assertTrue(resource.isClosed);

		assertTrue(moduleInitRan);
	}

	@Test
	void liftCanMakeUseOfParametersUsingArgumentModules() {
		Env env = Bootstrap.DEFAULT_ENV.with(Integer.class,	42); // setup some parameter
		Injector injector = Bootstrap.injector(env,
				TestExampleLiftAutoCloseableBindsModuleWith.class);

		// double check
		Lift<Injector> lift = injector.resolve(
				Lift.liftTypeOf(Injector.class));
		assertTrue(
				lift instanceof TestExampleLiftAutoCloseableBindsModuleWith);
		TestExampleLiftAutoCloseableBindsModuleWith module = (TestExampleLiftAutoCloseableBindsModuleWith) lift;
		assertNotNull(module.setup);
		assertEquals(42, module.setup.intValue());
	}
}
