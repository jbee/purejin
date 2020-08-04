package test.integration.bind;

import org.junit.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.DefaultScopes;

import static org.junit.Assert.assertEquals;

public class TestLinker {

	static class TwiceInstalledModule extends BinderModule {

		@Override
		protected void declare() {
			bind(Integer.class).to(42);
		}

	}

	private static class LinkerBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(DefaultScopes.class);
			install(new TwiceInstalledModule());
			install(new TwiceInstalledModule());
		}

	}

	/**
	 * A monomodal module has just one initial state (or no state). Therefore it
	 * can be determined that installing it twice or more does not make sense.
	 */
	@Test
	public void thatMonomodalModulesCanBeInstalledTwice() {
		Injector injector = Bootstrap.injector(LinkerBundle.class);
		assertEquals(42, injector.resolve(Integer.class).intValue());
	}
}
