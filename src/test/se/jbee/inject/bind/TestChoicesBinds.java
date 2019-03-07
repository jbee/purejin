package se.jbee.inject.bind;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bundle;
import se.jbee.inject.bootstrap.ChoiceBootstrapperBundle;
import se.jbee.inject.bootstrap.Module;
import se.jbee.inject.config.Choices;
import se.jbee.inject.config.Globals;

/**
 * The test demonstrates how to use {@link Choices} to allow different
 * bootstrapping depended on a setting that can be determined before the
 * bootstrapping and that is constant from that moment on. In this example it is
 * the machine the application is running on.
 *
 * Again this technique should avoid if-statements in the {@link Bundle}s and
 * {@link Module}s itself to get manageable and predictable sets of
 * configurations that can be composed easily using arguments to the
 * bootstrapping process itself.
 *
 * In this example we use {@link Binder#multibind(Class)}s to show that just one
 * of them has been bootstrapped depending on the value we defined in the
 * {@link Choices} before bootstrapping.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestChoicesBinds {

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
	 * value has been defined in the {@link Choices} so that it is actually
	 * <code>null</code>.
	 */
	private static class MachineBundle
			extends ChoiceBootstrapperBundle<Machine> {

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
		Choices choices = Choices.NONE.choose(actualChoice);
		Injector injector = Bootstrap.injector(ModularBindsBundle.class,
				Globals.STANDARD.with(choices));
		assertArrayEquals(new String[] { expected },
				injector.resolve(String[].class));
	}
}
