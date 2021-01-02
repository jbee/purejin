package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.EnvModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.ContractsBy;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static se.jbee.inject.lang.Type.raw;

/**
 * This example shows how a implementation {@link Class} gets bound to its
 * different contracts using {@link Binder#withContractAccess()} and {@link
 * ContractsBy} strategy.
 */
class TestExampleLocalContractsByBinds {

	private static class TestExampleLocalContractsByBindsEnvModule
			extends EnvModule {

		@Override
		protected void declare() {
			// our default
			bindContractsBy().to(ContractsBy.PROTECTIVE);

			// our exception
			bindContractsByOf(String.class).to(ContractsBy.SUPER);
		}
	}

	private static class TestExampleLocalContractsByBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			withContractAccess().bind(String.class).to("42");
			withContractAccess().bind(BigInteger.class).to(BigInteger.valueOf(42L));
		}
	}

	private final Injector context = Bootstrap.injector(
			Bootstrap.env(TestExampleLocalContractsByBindsEnvModule.class),
			TestExampleLocalContractsByBindsModule.class);

	@Test
	void protectiveContractsByAppliesOutsideOfJavaLang() {
		assertEquals(BigInteger.valueOf(42L), context.resolve(
				raw(Comparable.class).parameterized(BigInteger.class)));
		assertThrows(UnresolvableDependency.ResourceResolutionFailed.class,
				() -> context.resolve(BigInteger.class));
		assertThrows(UnresolvableDependency.ResourceResolutionFailed.class,
				() -> context.resolve(Number.class));
	}

	@Test
	void superContractsByAppliedWithinJavaLang() {
		assertEquals("42", context.resolve(String.class));
		assertEquals("42", context.resolve(CharSequence.class));
	}
}
