package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Injectron;
import se.jbee.inject.bootstrap.Binding;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Inspect;
import se.jbee.inject.bootstrap.Macro;
import se.jbee.inject.bootstrap.Macros;
import se.jbee.inject.bootstrap.Module;
import se.jbee.inject.config.Globals;

/**
 * Demonstrates how to use {@link Macro}s to customize the and binding automatics.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestMacroBinds {

	private static class MacroBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( String.class ).to( "anser" );
			bind( Integer.class ).to( 42 );
			bind( Boolean.class ).to( true );
		}

	}

	private static final class CountMacro
			implements Macro<Binding<?>> {

		int expands = 0;

		CountMacro() {
			// make visible
		}

		@Override
		public <T> Module expand( Binding<T> binding, Binding<?> value ) {
			expands++;
			return Macros.NO_OP;
		}

	}

	@Test
	public void thatBindingsCanJustBeCounted() {
		CountMacro count = new CountMacro();
		Injector injector = Bootstrap.injector( MacroBindsModule.class,
				Bindings.bindings( Macros.DEFAULT.use( count ), Inspect.DEFAULT ), Globals.STANDARD );
		assertEquals( 3, count.expands );
		assertEquals( 0, injector.resolve( Dependency.dependency( Injectron[].class ) ).length );
	}
}
