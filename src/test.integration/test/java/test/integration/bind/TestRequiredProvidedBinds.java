package test.integration.bind;

import org.junit.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.bind.Module;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.Assert.*;

/**
 * The test shows very loose coupling using {@link BinderModule#require(Class)}
 * and {@link BinderModule#provide(Class, se.jbee.inject.Parameter...)}
 * bindings.
 *
 * Here a {@link Module} just expresses the need for a particular implementation
 * of an interface while one or more other modules express the ability to
 * deliver an implementation if needed. Interface and implementation are
 * connected without that a particular module points out both of them. This is
 * very helpful to compose an application out of parts that do not yet know each
 * other.
 */
public class TestRequiredProvidedBinds {

	private interface ExampleService {
		// a classic singleton bean
	}

	private static class ExampleServiceImpl implements ExampleService {
		// and its implementation
	}

	private static class ExplicitExampleService implements ExampleService {
		// this is bound explicit in one test and should replace the provided impl. above
	}

	private static class UnusedImpl {
		// just something we provide but do not require
	}

	private static class RequirementModule extends BinderModule {

		@Override
		protected void declare() {
			bind(Integer.class).to(42);
			require(ExampleService.class);
		}

	}

	private static class ProvidingModule extends BinderModule {

		@Override
		protected void declare() {
			provide(ExampleServiceImpl.class);
			provide(UnusedImpl.class);
		}

	}

	private static class RequiredProvidedBindsBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(RequirementModule.class);
			install(ProvidingModule.class);
		}
	}

	private static class ExplicitBindModule extends BinderModule {

		@Override
		protected void declare() {
			bind(ExampleService.class).to(ExplicitExampleService.class);
		}
	}

	private static class ExplicitBindBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(RequiredProvidedBindsBundle.class);
			install(ExplicitBindModule.class);
		}

	}

	@Test(expected = NoResourceForDependency.class)
	public void thatNotProvidedRequiredBindThrowsException() {
		Bootstrap.injector(RequirementModule.class);
	}

	@Test
	public void thatRequirementIsFulfilledByProvidedBind() {
		Injector injector = Bootstrap.injector(
				RequiredProvidedBindsBundle.class);
		assertNotNull(injector.resolve(ExampleService.class));
	}

	@Test
	public void thatUnusedProvidedBindIsNotAddedToInjectorContext() {
		Injector injector = Bootstrap.injector(
				RequiredProvidedBindsBundle.class);
		try {
			injector.resolve(UnusedImpl.class);
			fail("Should not be bound and therefore throw below exception");
		} catch (NoResourceForDependency e) {
			// expected this
		} catch (Exception e) {
			fail("Expected another exception but got: " + e);
		}
	}

	@Test
	public void thatAnExplicitBindReplacesTheProvidedImplementation() {
		Injector injector = Bootstrap.injector(ExplicitBindBundle.class);
		ExampleService s = injector.resolve(ExampleService.class);
		assertTrue(s instanceof ExplicitExampleService);
	}
}
