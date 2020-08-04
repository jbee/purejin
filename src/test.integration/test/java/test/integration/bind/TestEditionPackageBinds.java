package test.integration.bind;

import org.junit.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.Packages;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.Edition;
import se.jbee.inject.defaults.DefaultsBundle;

import static org.junit.Assert.*;
import static se.jbee.inject.Packages.*;

/**
 * The test demonstrates how to assemble different editions based on the
 * {@link Packages} the {@link Bundle}s (here in form of a {@link BinderModule})
 * are located.
 *
 * @see TestEditionFeatureBinds
 *
 * @author jan
 */
public class TestEditionPackageBinds {

	static final class EditionPackageBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(Integer.class).to(42);
		}

	}

	static final class EditionPackageBindsBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(EditionPackageBindsModule.class);
		}

	}

	@Test
	public void editionsCanBeUsedToInstallBundlesPackageDependent() {
		// no edition
		Injector injector = Bootstrap.injector(EditionPackageBindsBundle.class);
		assertEquals(42, injector.resolve(int.class).intValue());

		// an edition without the module in this test
		Env env = Bootstrap.ENV.with(Edition.class,
				Edition.includes(subPackagesOf(TestEditionPackageBinds.class) //
						.and(packageOf(DefaultsBundle.class)
						.and(packageOf(Bootstrap.class)))));
		injector = Bootstrap.injector(env, EditionPackageBindsBundle.class);
		try {
			Integer res = injector.resolve(int.class);
			assertNull(res);
			fail("Should have thrown exception since EditionPackageBindsModule should not have been installed");
		} catch (NoResourceForDependency e) {
			// expected this
		}

		// an edition including the module in this test
		env = Bootstrap.ENV.with(Edition.class, Edition.includes(
				packageAndSubPackagesOf(TestEditionPackageBinds.class) //
						.and(packageOf(DefaultsBundle.class))
						.and(packageOf(Bootstrap.class))));
		injector = Bootstrap.injector(env, EditionPackageBindsBundle.class);
		assertEquals(42, injector.resolve(int.class).intValue());
	}
}
