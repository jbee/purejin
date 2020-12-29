package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.Packages;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.Edition;
import se.jbee.inject.defaults.DefaultsBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static se.jbee.inject.Packages.*;

/**
 * The test demonstrates how to assemble different editions based on the
 * {@link Packages} the {@link Bundle}s (here in form of a {@link BinderModule})
 * are located.
 *
 * @see TestFeatureEditionFeatureBinds
 */
class TestFeatureEditionPackageBinds {

	private static final class TestFeatureEditionPackageBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind(Integer.class).to(42);
		}
	}

	@Test
	void editionsCanBeUsedToInstallBundlesPackageDependent() {
		// no edition
		Injector injector = Bootstrap.injector(
				TestFeatureEditionPackageBindsModule.class);
		assertEquals(42, injector.resolve(int.class).intValue());

		// an edition without the module in this test
		Env env = Bootstrap.DEFAULT_ENV.with(Edition.class,
				Edition.includes(subPackagesOf(TestFeatureEditionPackageBinds.class) //
						.and(packageOf(DefaultsBundle.class)
						.and(packageOf(Bootstrap.class)))));

		Injector context = Bootstrap.injector(env, TestFeatureEditionPackageBindsModule.class);
		assertThrows(UnresolvableDependency.ResourceResolutionFailed.class, () -> context.resolve(int.class),
				"Should have thrown exception since EditionPackageBindsModule should not have been installed");

		// an edition including the module in this test
		env = Bootstrap.DEFAULT_ENV.with(Edition.class, Edition.includes(
				packageAndSubPackagesOf(TestFeatureEditionPackageBinds.class) //
						.and(packageOf(DefaultsBundle.class))
						.and(packageOf(Bootstrap.class))));
		injector = Bootstrap.injector(env, TestFeatureEditionPackageBindsModule.class);
		assertEquals(42, injector.resolve(int.class).intValue());
	}
}
