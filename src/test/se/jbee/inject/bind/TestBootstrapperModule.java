package se.jbee.inject.bind;

import static org.junit.Assert.assertSame;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Bootstrapper;
import se.jbee.inject.bootstrap.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bundle;
import se.jbee.inject.config.Globals;
import se.jbee.inject.config.Options;

/**
 * Test that illustrates that the {@link Bootstrap} process adds bindings such
 * as binding {@link Globals}.
 */
public class TestBootstrapperModule {

	static class UninstallingBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			uninstall(Bootstrap.getBootstrapperBundle());
		}
	}

	static class EmptyBundle implements Bundle {

		@Override
		public void bootstrap(Bootstrapper bootstrap) {
			// do nothing
		}
	}

	@Test
	public void globalsAreBoundByBootstrapper() {
		Globals expected = Globals.STANDARD.with(
				Options.NONE.set(String.class, "custom"));
		Injector injector = Bootstrap.injector(EmptyBundle.class, expected);
		Globals actual = injector.resolve(Globals.class);
		assertSame(expected, actual);
	}

	@Test(expected = UnresolvableDependency.NoCaseForDependency.class)
	public void standardBootstrapperModuleCanBeUninstalled() {
		Injector injector = Bootstrap.injector(UninstallingBundle.class);
		injector.resolve(Globals.class);
	}
}
