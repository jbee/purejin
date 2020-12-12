package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A very small example showing that stateless {@link se.jbee.inject.bind.Module}
 * classes can be installed multiple times using different actual instances
 * without causing errors because of duplicate (clashing) bindings.
 * <p>
 * This is because they are recognised as stateless and only the first install
 * does have an effect. Further installs are ignored.
 * <p>
 * The situation would be different if the installed {@link
 * se.jbee.inject.bind.Module} has state. In such case is must be assumed that
 * this is intentional reuse of the same code to create different bindings based
 * on the internal state. Such {@link se.jbee.inject.bind.Module} are all
 * installed. Should these lead to clashing bindings this causes the
 * bootstrapping to fail with an exception.
 */
class TestBasicMultiInstallBinds {

	private static class TwiceInstalledModule extends BinderModule {

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
	 * A stateless module that just has one initial state (or no state) can be
	 * installed multiple times without causing an error.
	 */
	@Test
	void statelessModulesCanBeInstalledTwice() {
		Injector injector = Bootstrap.injector(TestMultiInstallBundle.class);
		assertEquals(42, injector.resolve(Integer.class).intValue());
	}
}
