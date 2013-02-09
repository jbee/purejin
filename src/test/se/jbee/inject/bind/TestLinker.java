package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Dependency.dependency;

import org.junit.Test;

import se.jbee.inject.Injector;

public class TestLinker {

	static class TwiceInstalledModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( Integer.class ).to( 42 );
		}

	}

	private static class LinkerBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install( new TwiceInstalledModule() );
			install( new TwiceInstalledModule() );
		}

	}

	@Test
	public void thatStatelessModulesCanBeInstalledTwice() {
		Injector injector = Bootstrap.injector( LinkerBundle.class );
		assertEquals( 42, injector.resolve( dependency( Integer.class ) ).intValue() );
	}
}
