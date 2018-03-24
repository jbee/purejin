package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static se.jbee.inject.Packages.packageAndSubPackagesOf;
import static se.jbee.inject.Packages.subPackagesOf;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.Packages;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bundle;
import se.jbee.inject.config.Globals;

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
		injector = Bootstrap.injector(EditionPackageBindsBundle.class,
				Globals.STANDARD.edition(subPackagesOf(TestEditionPackageBinds.class)));
		try {
			Integer res = injector.resolve(int.class);
			assertNull(res);
			fail("Should have thrown exception since EditionPackageBindsModule should not have been installed");
		} catch (NoResourceForDependency e) {
			// expected this
		}

		// an edition including the module in this test
		injector = Bootstrap.injector(EditionPackageBindsBundle.class,
				Globals.STANDARD.edition(packageAndSubPackagesOf(TestEditionPackageBinds.class)));
		assertEquals(42, injector.resolve(int.class).intValue());
	}
}
