package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.container.Typecast.initialiserTypeOf;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.Globals;
import se.jbee.inject.config.Presets;
import se.jbee.inject.container.Initialiser;

/**
 * The tests demonstrates how the {@link Initialiser} and
 * {@link Binder#initbind()} can be used to e.g. install a "shutdown hook" that
 * would automatically close all {@link AutoCloseable}s. Here the
 * "shutdown hook" of course is simulated so we can test for it being invoked.
 * In a real scenario one would use {@link Runtime#addShutdownHook(Thread)}.
 */
public class TestInitialiserBinds {

	static final class TestInitialiserBindsModule extends BinderModule implements Initialiser<Injector> {

		@Override
		protected void declare() {
			initbind().to(this);
			initbind().to(AutoCloseableInitialiser.class);
			construct(SingletonResource.class);
		}

		@Override
		public void init(Injector context) {
			// just to show that one could use the module itself as well
			// this e.g. allows to pass down and use setup data by using
			// PresetModule's as shown with TestInitialiserBindsPresetModule
			moduleInitRan = true;
		}

	}

	static final class TestInitialiserBindsPresetModule extends BinderModuleWith<Integer> implements Initialiser<Injector> {

		Integer setup;

		@Override
		protected void declare(Integer setup) {
			//... some binds
			initbind().to(this);

			this.setup = setup;
		}

		@Override
		public void init(Injector context) {
			assertNotNull(setup);
			// use setup to initialize something
		}
	}

	static class AutoCloseableInitialiser implements Initialiser<Injector> {

		public AutoCloseableInitialiser(AutoCloseable[] autoCloseables) {
			// since this instance is created by the container it is also properly injected.
			// so this can be used to receive instances that should be initialized as well
			// in this case we just did it to show the possibility but it is not needed
			assertEquals(0, autoCloseables.length);
			// however in this case there is no way to express the type
			// "? extends AutoCloseable[]" in the java language and
			// AutoCloseable[] is empty since we did not bind anything
			// explicitly or implicitly to the AutoCloseable interface
			// in such cases this must be done in the init method as shown below
		}

		@Override
		public void init(Injector context) {
			// by the use of upper bound we receive all implementing classes
			// even though they have not be bound explicitly for AutoCloseable.
			AutoCloseable[] autoCloseables = context.resolve(raw(AutoCloseable[].class).asUpperBound());
			assertTrue(autoCloseables.length > 0);
			shutdownHookMock = () -> {
				for (AutoCloseable a : autoCloseables)
					try {
						a.close();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
			};
		}

	}

	/**
	 * This class simulates some singleton that needs to be closed like a DB
	 * connection instance.
	 */
	static class SingletonResource implements AutoCloseable {

		boolean isClosed = false;

		@Override
		public void close() throws Exception {
			isClosed = true;
		}

	}

	static Runnable shutdownHookMock;
	static boolean moduleInitRan = false;

	@Test
	public void initialisersCanBeUsedToCloseAnyAutoCloseable() {
		Injector injector = Bootstrap.injector(TestInitialiserBindsModule.class);

		assertNotNull(shutdownHookMock);
		@SuppressWarnings("resource")
		SingletonResource resource = injector.resolve(SingletonResource.class);
		assertNotNull(resource);
		assertFalse(resource.isClosed);
		shutdownHookMock.run();
		assertTrue(resource.isClosed);

		assertTrue(moduleInitRan);
	}

	@Test
	public void initialisersCanMakeUseOfParammetersUsingPresetModules() {
		Globals globals = Globals.STANDARD.presets(Presets.EMPTY.preset(Integer.class, 42)); // setup some parameter
		Injector injector = Bootstrap.injector(TestInitialiserBindsPresetModule.class, globals);

		// double check
		Initialiser<Injector> initialiser = injector.resolve(initialiserTypeOf(Injector.class));
		assertTrue(initialiser instanceof TestInitialiserBindsPresetModule);
		TestInitialiserBindsPresetModule module = (TestInitialiserBindsPresetModule) initialiser;
		assertNotNull(module.setup);
		assertEquals(42, module.setup.intValue());
	}
}
