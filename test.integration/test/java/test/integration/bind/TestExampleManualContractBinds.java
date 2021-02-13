package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Lift;
import se.jbee.inject.UnresolvableDependency.ResourceResolutionFailed;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.EnvModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.PublishesBy;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static se.jbee.lang.Type.raw;

/**
 * A example that shows how API contract for {@link Binder#withPublishedAccess()}
 * are explicitly declared using {@link EnvModule#addPublishedAPI(Class)}.
 * <p>
 * Published APIs are all the types a bound type is assignable to and that are
 * considered a "contract". So the question becomes what types are considered
 * APIs. This is controlled by the {@link PublishesBy} strategy found in
 * the {@link se.jbee.inject.Env}.
 */
class TestExampleManualContractBinds {

	/**
	 * Contract {@link Class}es must be added using {@link #addPublishedAPI(Class)}
	 * in a module that adds to the {@link se.jbee.inject.Env} used to bootstrap
	 * the {@link Injector} so that at the point we do bootstrap the {@link
	 * Injector} we already know all APIs considered a contract.
	 */
	private static class TestExampleManualContractBindsEnvModule
			extends EnvModule {

		@Override
		protected void declare() {
			// real application has many of (in different modules):
			addPublishedAPI(Serializable.class); // consider Serializable as a type that is bound if implemented by any bind using "withPublishedAccess"

			// real application has one declaration of (in general module):
			// by default no type is considered a contract
			bind(PublishesBy.class).toScoped(PublishesBy.NONE);
			// but we do decorate the default behaviour with the one accepting all in the set of declared contract
			// those are all we added somewhere using: addContract(Class)
			bind(Lift.liftTypeOf(PublishesBy.class))
					.to(PublishesBy::liftDeclaredSet);
		}
	}

	private static class TestExampleManualContractBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			withPublishedAccess().bind(Integer.class).to(42);
		}
	}

	private final Injector context = Bootstrap.injector(
			Bootstrap.env(TestExampleManualContractBindsEnvModule.class),
			TestExampleManualContractBindsModule.class);

	@Test
	void explicitlyAddedApisAreBound() {
		assertEquals(42, context.resolve(Serializable.class));
	}

	@Test
	void otherInterfacesAreNotBound() {
		assertThrows(ResourceResolutionFailed.class,
				() -> context.resolve(
						raw(Comparable.class).parameterized(Integer.class)));
	}

	@Test
	void exactTypeIsNotBound() {
		assertThrows(ResourceResolutionFailed.class,
				() -> context.resolve(Integer.class));
	}
}
