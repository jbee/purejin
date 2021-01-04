package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.PublishesBy;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This example shows how to build a concept where the contracts are annotated
 * with annotation like {@link Contract} which marks interfaces or {@link
 * ContractsProvided} which is put on a service to point out interfaces
 * implemented.
 * <p>
 * This builds upon the concept of {@link PublishesBy} which selects the
 * super-classes and super-interfaces of bound types if they are bound as {@link
 * Binder#withPublishedAccess()}.
 * <p>
 * In this example we also create a utility extension {@link ProjectBaseModule}
 * of the default {@link BinderModule} which is assumed to be used as base class
 * for the application project. It adds a convenience method {@link
 * ProjectBaseModule#addBean(Class)} which internally creates the {@link
 * Binder#withPublishedAccess()}.
 *
 * This is only meant to show a more realistic way how a project would use
 * annotation guided contracts.
 *
 * @see TestExampleLocalPublishesByBinds
 */
class TestExampleAnnotatedContractsBinds {

	@Documented
	@Target(TYPE)
	@Retention(RUNTIME)
	@interface Contract {}

	@Documented
	@Target(TYPE)
	@Retention(RUNTIME)
	@interface ContractsProvided {
		Class<?>[] value();
	}

	@Contract
	interface MyContract {

	}

	interface AnotherContract {

	}

	@ContractsProvided(AnotherContract.class)
	public static class SomeService
			implements MyContract, AnotherContract, Runnable {

		@Override
		public void run() {

		}
	}

	private static abstract class ProjectBaseModule extends BinderModule {

		void addBean(Class<?> bean) {
			withPublishedAccess().bind(bean).toConstructor();
		}
	}

	private static class SomeServiceModule extends ProjectBaseModule {

		@Override
		protected void declare() {
			addBean(SomeService.class);
		}
	}

	private final Injector context = Bootstrap.injector(
			Bootstrap.DEFAULT_ENV.with(PublishesBy.class,
					PublishesBy.SUPER.annotatedWith(Contract.class).or(
							PublishesBy.SUPER.annotatedWith(
									ContractsProvided.class,
									ContractsProvided::value))),
			SomeServiceModule.class);

	@Test
	void annotatedContractInterfacesAreBound() {
		assertEquals(SomeService.class,
				context.resolve(MyContract.class).getClass());
	}

	@Test
	void referencedContractInterfacesAreBound() {
		assertEquals(SomeService.class,
				context.resolve(AnotherContract.class).getClass());
	}

	@Test
	void notAnnotatedInterfacesAreNotBound() {
		assertThrows(UnresolvableDependency.ResourceResolutionFailed.class,
				() -> context.resolve(Runnable.class));
	}
}

