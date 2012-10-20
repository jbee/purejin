package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Type.raw;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Ignore;
import org.junit.Test;

import de.jbee.inject.Injector;
import de.jbee.inject.Type;

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

	@Test
	@Ignore
	public void thatLowerBoundsCanBeUsedToGetAnAvailableResource() {
		injector.resolve( dependency( Type.raw( Number.class ).asLowerBound() ).named( "foo" ) );
	}

	private <T> void assertInjects( T expected, Type<? extends T> dependencyType ) {
		assertThat( injector.resolve( dependency( dependencyType ) ), is( expected ) );
	}
}
