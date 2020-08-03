package se.jbee.inject.bootstrap;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.Module;
import se.jbee.inject.bind.Toggled;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.binder.TogglerBundle;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * The test demonstrates how to use {@link Toggled} and the
 * {@link TogglerBundle} to allow different bootstrapping depended on a toggle
 * property set in the {@link Env}.
 *
 * This technique should avoid if-statements in the {@link Bundle}s and
 * {@link Module}s itself to get manageable and predictable sets of
 * configurations that can be composed easily using arguments to the
 * bootstrapping process itself.
 *
 * In this example we use {@link Binder#multibind(Class)}s to show that just one
 * of them has been bootstrapped depending on the value we defined in the
 * toggled property before bootstrapping.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestToggledBinds {

	private enum Machine {
		LOCALHOST, WORKER_1
	}

	private static class ModularBindsBundle extends BootstrapperBundle {

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
	private static class MachineBundle extends TogglerBundle<Machine> {

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
	public void thatBundleOfTheGivenConstGotBootstrappedAndOthersNot() {
		assertChoiceResolvedToValue(Machine.LOCALHOST, "on-localhost");
		assertChoiceResolvedToValue(Machine.WORKER_1, "on-worker-1");
	}

	@Test
	public void thatBundleOfUndefinedConstGotBootstrappedAndOthersNot() {
		assertChoiceResolvedToValue(null, "on-generic");
	}

	private static void assertChoiceResolvedToValue(Machine actualChoice,
			String expected) {
		Env env = Bootstrap.ENV.withToggled(Machine.class, actualChoice);
		Injector injector = Bootstrap.injector(env, ModularBindsBundle.class);
		assertArrayEquals(new String[] { expected },
				injector.resolve(String[].class));
	}
}
