package se.jbee.inject.bind;

import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.bind.AssertInjects.assertEqualSets;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BootstrapperBundle;
import se.jbee.inject.bootstrap.ModularBootstrapperBundle;
import se.jbee.inject.config.Globals;
import se.jbee.inject.config.Options;

public class TestMultipleOptionChoicesBinds {

	private static enum Choices {
		A,
		B,
		C,
		D,
		E
	}

	private static class A
			extends BinderModule {

		@Override
		protected void declare() {
			multibind( String.class ).to( "A" );
		}

	}

	private static class B
			extends BinderModule {

		@Override
		protected void declare() {
			multibind( String.class ).to( "B" );
		}

	}

	private static class C
			extends BinderModule {

		@Override
		protected void declare() {
			multibind( String.class ).to( "C" );
		}
	}

	private static class D
			extends BinderModule {

		@Override
		protected void declare() {
			multibind( String.class ).to( "D" );
		}

	}

	private static class E
			extends BinderModule {

		@Override
		protected void declare() {
			multibind( String.class ).to( "E" );
		}

	}

	private static class ChoicesBundle
			extends ModularBootstrapperBundle<Choices> {

		@Override
		protected void bootstrap() {
			install( A.class, Choices.A );
			install( B.class, Choices.B );
			install( C.class, Choices.C );
			install( D.class, Choices.D );
			install( E.class, Choices.E );
		}
	}

	private static class RootBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install( ChoicesBundle.class, Choices.class );
		}

	}

	@Test
	public void thatMultipleOptionChoicesArePossible() {
		Options options = Options.STANDARD.chosen( Choices.A, Choices.D );
		Globals globals = Globals.STANDARD.options( options );
		Injector injector = Bootstrap.injector( RootBundle.class, globals );
		assertEqualSets( new String[] { "A", "D" }, injector.resolve( dependency( String[].class ) ) );
	}
}
