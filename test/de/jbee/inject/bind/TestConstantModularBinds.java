package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.jbee.inject.DependencyResolver;
import de.jbee.inject.bind.Bootstrapper.ModularBootstrapper;

public class TestConstantModularBinds {

	private static enum Machine
			implements Const {
		LOCALHOST,
		WORKER_1
	}

	private static class ConstantModularBindsBundle
			extends DirectBundle {

		@Override
		protected void bootstrap() {
			install( MachineBundle.class, Machine.class );
		}

	}

	private static class MachineBundle
			implements ModularBundle<Machine> { //TODO base-class that offers small nice builder ?

		@Override
		public void bootstrap( ModularBootstrapper<Machine> bootstrap ) {
			bootstrap.install( GenericMachineBundle.class, null );
			bootstrap.install( LocalhostBundle.class, Machine.LOCALHOST );
			bootstrap.install( Worker1Bundle.class, Machine.WORKER_1 );
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
		DependencyResolver injector = Bootstrap.injector( ConstantModularBindsBundle.class,
				Edition.FULL, constants );
		String[] actual = injector.resolve( dependency( String[].class ) );
		assertThat( actual, is( new String[] { expected } ) );
	}
}
