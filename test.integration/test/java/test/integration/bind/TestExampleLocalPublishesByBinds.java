package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.EnvModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.PublishesBy;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static se.jbee.lang.Type.raw;

/**
 * This example shows how a implementation {@link Class} gets bound to its
 * different APIs using {@link Binder#withPublishedAccess()} and {@link
 * PublishesBy} strategy.
 *
 * @see TestExampleAnnotatedContractsBinds
 */
class TestExampleLocalPublishesByBinds {

	private static class TestExampleLocalContractsByBindsEnvModule
			extends EnvModule {

		@Override
		protected void declare() {
			// our default
			publish().to(PublishesBy.PROTECTIVE);

			// our exception
			publish(String.class).to(PublishesBy.SUPER);
		}
	}

	private static class TestExampleLocalPublishesByBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			withPublishedAccess().bind(String.class).to("42");
			withPublishedAccess().bind(BigInteger.class).to(BigInteger.valueOf(42L));
		}
	}

	private final Injector context = Bootstrap.injector(
			Bootstrap.env(TestExampleLocalContractsByBindsEnvModule.class),
			TestExampleLocalPublishesByBindsModule.class);

	@Test
	void protectivePublishesByAppliesOutsideOfJavaLang() {
		assertEquals(BigInteger.valueOf(42L), context.resolve(
				raw(Comparable.class).parameterized(BigInteger.class)));
		assertThrows(UnresolvableDependency.ResourceResolutionFailed.class,
				() -> context.resolve(BigInteger.class));
		assertThrows(UnresolvableDependency.ResourceResolutionFailed.class,
				() -> context.resolve(Number.class));
	}

	@Test
	void superPublishesByAppliedWithinJavaLang() {
		assertEquals("42", context.resolve(String.class));
		assertEquals("42", context.resolve(CharSequence.class));
	}
}
