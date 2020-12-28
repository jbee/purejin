package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The test shows how to establish loose coupling between software modules using
 * {@link BinderModule#require(Class)} and {@link BinderModule#provide(Class,
 * se.jbee.inject.Hint...)}.
 * <p>
 * One software module expresses the need for a particular implementation of an
 * interface while another software module express the ability to provide an
 * implementation if needed. Interface and implementation are connected without
 * that either of the modules points to the other. Their both are unaware of
 * each other yet the {@link Injector} context connects the two.
 * <p>
 * Such indirect coupling can be useful for example when building a plugin
 * system.
 */
class TestBasicRequiredProvidedBinds {

	interface ExampleService {
		// a classic singleton bean
	}

	public static class ExampleServiceImpl implements ExampleService {
		// and its implementation
	}

	public static class ExplicitExampleService implements ExampleService {
		// this is bound explicit in one test and should replace the provided impl. above
	}

	public static class UnusedImpl {
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

	private static class TestBasicRequiredProvidedBindsBundle
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

	/**
	 * Required and provides can even be used in the presence of an explicit
	 * binding for the provided interface. In that case the explicit binding
	 * replaces the loose coupling.
	 */
	private static class ExplicitBindBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(TestBasicRequiredProvidedBindsBundle.class);
			install(ExplicitBindModule.class);
		}
	}

	@Test
	void notProvidedRequiredBindThrowsException() {
		assertThrows(UnresolvableDependency.ResourceResolutionFailed.class,
				() -> Bootstrap.injector(RequirementModule.class));
	}

	@Test
	void requirementIsFulfilledByProvidedBind() {
		Injector injector = Bootstrap.injector(
				TestBasicRequiredProvidedBindsBundle.class);
		assertNotNull(injector.resolve(ExampleService.class));
	}

	@Test
	void unusedProvidedBindIsNotAddedToInjectorContext() {
		Injector context = Bootstrap.injector(
				TestBasicRequiredProvidedBindsBundle.class);
		assertThrows(UnresolvableDependency.ResourceResolutionFailed.class,
				() -> context.resolve(UnusedImpl.class),
				"Should not be bound and therefore throw below exception");
	}

	@Test
	void anExplicitBindReplacesTheProvidedImplementation() {
		Injector context = Bootstrap.injector(ExplicitBindBundle.class);
		ExampleService s = context.resolve(ExampleService.class);
		assertTrue(s instanceof ExplicitExampleService);
	}
}
