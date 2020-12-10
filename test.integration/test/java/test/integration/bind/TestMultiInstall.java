package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestMultiInstall {

	static class TwiceInstalledModule extends BinderModule {

		@Override
		protected void declare() {
			bind(Integer.class).to(42);
		}

	}

	private static class TestMultiInstallBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			installDefaults();
			install(new TwiceInstalledModule());
			install(new TwiceInstalledModule());
		}

	}

	/**
	 * A monomodal module has just one initial state (or no state). Therefore it
	 * can be determined that installing it twice or more does not make sense.
	 */
	@Test
	void thatMonomodalModulesCanBeInstalledTwice() {
		Injector injector = Bootstrap.injector(TestMultiInstallBundle.class);
		assertEquals(42, injector.resolve(Integer.class).intValue());
	}
}
