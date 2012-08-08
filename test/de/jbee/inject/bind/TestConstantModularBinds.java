package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.jbee.inject.Injector;

/**
 * The test demonstrates how to use {@link Constants} and {@link Const} types to allow different
 * bootstrapping depended on a setting that can be determined before the bootstrapping and that is
 * constant from that moment on. In this example it is the machine the application is running on.
 * 
 * Again this technique should avoid if-statements in the {@link Bundle}s and {@link Module}s itself
 * to get manageable and predictable sets of configurations that can be composed easily using
 * arguments to the bootstrapping process itself.
 * 
 * In this example we use {@link Binder#multibind(Class)}s to show that just one of them has been
 * bootstrapped depending on the value we defined in the {@link Constants} before bootstrapping.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public class TestConstantModularBinds {

	private static enum Machine
			implements Const {
		LOCALHOST,
		WORKER_1
	}

	private static class ConstantModularBindsBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install( MachineBundle.class, Machine.class );
		}

	}

	/**
	 * The {@link GenericMachineBundle} will be used when no {@link Machine} {@link Const} has been
	 * defined in the {@link Constants} so that it is actually <code>null</code>.
	 */
	private static class MachineBundle
			extends ModularBootstrapperBundle<Machine> {

		@Override
		protected void bootstrap() {
			install( GenericMachineBundle.class, null );
			install( LocalhostBundle.class, Machine.LOCALHOST );
			install( Worker1Bundle.class, Machine.WORKER_1 );
		}
	}

	private static class GenericMachineBundle
			extends BinderModule {

		@Override
		protected void declare() {
			multibind( String.class ).to( "on-generic" );
		}

	}

	private static class Worker1Bundle
			extends BinderModule {

		@Override
		protected void declare() {
			multibind( String.class ).to( "on-worker-1" );
		}

	}

	private static class LocalhostBundle
			extends BinderModule {

		@Override
		protected void declare() {
			multibind( String.class ).to( "on-localhost" );
		}

	}

	@Test
	public void thatBundleOfTheGivenConstGotBootstrappedAndOthersNot() {
		assertResolved( Machine.LOCALHOST, "on-localhost" );
		assertResolved( Machine.WORKER_1, "on-worker-1" );
	}

	@Test
	public void thatBundleOfUndefinedConstGotBootstrappedAndOthersNot() {
		assertResolved( null, "on-generic" );
	}

	private void assertResolved( Machine actualConstant, String expected ) {
		Constants constants = Constants.NONE.def( actualConstant );
		Injector injector = Bootstrap.injector( ConstantModularBindsBundle.class, Edition.FULL,
				constants );
		String[] actual = injector.resolve( dependency( String[].class ) );
		assertThat( actual, is( new String[] { expected } ) );
	}
}
