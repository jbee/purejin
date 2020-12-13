package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.Module;
import se.jbee.inject.bind.Toggled;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.binder.BundleFor;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Environment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * The test demonstrates how to use {@link Toggled} and the {@link BundleFor} to
 * allow different bootstrapping depended on a toggle property set in the {@link
 * Env}.
 * <p>
 * This technique avoids using if-statements in the {@link Bundle}s and {@link
 * Module}s itself to get manageable and predictable sets of configurations that
 * can be composed easily using arguments to the bootstrapping process itself.
 * <p>
 * In this example we use {@link Binder#multibind(Class)}s to show that just one
 * of the bundles has been installed depending on the value we defined {@link
 * Env} for the {@link Machine} which is our toggle property.
 */
class TestBasicBundleForBinds {

	private enum Machine {
		LOCALHOST, WORKER_1
	}

	private static class TestBasicBundleForBindsBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(MachineBundle.class, Machine.class);
		}
	}

	/**
	 * The {@link GenericMachineBundle} will be used when no {@link Machine}
	 * value has been defined in the {@link Env} so that it is actually
	 * <code>null</code>.
	 */
	private static class MachineBundle extends BundleFor<Machine> {

		@Override
		protected void bootstrap() {
			install(GenericMachineBundle.class, null);
			install(LocalhostBundle.class, Machine.LOCALHOST);
			install(Worker1Bundle.class, Machine.WORKER_1);
		}
	}

	private static class GenericMachineBundle extends BinderModule {

		@Override
		protected void declare() {
			multibind(String.class).to("on-generic");
		}

	}

	private static class Worker1Bundle extends BinderModule {

		@Override
		protected void declare() {
			multibind(String.class).to("on-worker-1");
		}

	}

	private static class LocalhostBundle extends BinderModule {

		@Override
		protected void declare() {
			multibind(String.class).to("on-localhost");
		}

	}

	@Test
	void bundleOfTheGivenConstGotBootstrappedAndOthersNot() {
		assertChoiceResolvedToValue(Machine.LOCALHOST, "on-localhost");
		assertChoiceResolvedToValue(Machine.WORKER_1, "on-worker-1");
	}

	@Test
	void bundleOfUndefinedConstGotBootstrappedAndOthersNot() {
		assertChoiceResolvedToValue(null, "on-generic");
	}

	private static void assertChoiceResolvedToValue(Machine actualChoice,
			String expectedValue) {
		Env env = Environment.DEFAULT.withToggled(Machine.class, actualChoice);
		Injector context = Bootstrap.injector(env,
				TestBasicBundleForBindsBundle.class);
		assertArrayEquals(new String[] { expectedValue },
				context.resolve(String[].class));
	}
}
