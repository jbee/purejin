package se.jbee.inject.bind;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Type.raw;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BootstrapperBundle;

public class TestInstanceBinds {

	private static class InstanceBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( String.class ).to( "foobar" );
			bind( CharSequence.class ).to( "bar" );
			bind( Integer.class ).to( 42 );
			bind( Float.class ).to( 42.0f );
		}

	}

	private static class InstanceBindsBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			installAll( BuildinBundle.class );
			install( InstanceBindsModule.class );
		}

	}

	private final Injector injector = Bootstrap.injector( InstanceBindsBundle.class );

	@Test
	public void thatInstanceInjectedBasedOnTheDependencyType() {
		assertInjects( "bar", raw( CharSequence.class ) );
		assertInjects( "foobar", raw( String.class ) );
		assertInjects( 42, raw( Integer.class ) );
	}

	private <T> void assertInjects( T expected, Type<? extends T> dependencyType ) {
		assertThat( injector.resolve( dependency( dependencyType ) ), is( expected ) );
	}
}
