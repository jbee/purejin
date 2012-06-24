package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

import de.jbee.inject.Dependency;
import de.jbee.inject.DependencyResolver;

public class TestLocalisedBinds {

	private static class LocalisedBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( String.class ).to( "default" );
			inPackageOf( TestLocalisedBinds.class ).bind( String.class ).to( "test" );
		}

	}

	@Test
	public void test() {
		DependencyResolver injector = Bootstrap.injector( LocalisedBindsModule.class );
		Dependency<String> global = dependency( String.class );
		Dependency<String> local = global.into( TestLocalisedBinds.class );
		Dependency<String> somewhereElse = global.into( List.class );
		assertThat( injector.resolve( global ), is( "default" ) );
		assertThat( injector.resolve( local ), is( "test" ) );
		assertThat( injector.resolve( somewhereElse ), is( "default" ) );
	}
}
