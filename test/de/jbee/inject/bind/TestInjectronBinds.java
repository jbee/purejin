package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Name.named;
import static de.jbee.inject.util.Typecast.injectronTypeOf;
import static de.jbee.inject.util.Typecast.injectronsTypeOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

import de.jbee.inject.Dependency;
import de.jbee.inject.Injector;
import de.jbee.inject.Injectron;

public class TestInjectronBinds {

	private static class InjectronBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( String.class ).to( "foobar" );
			bind( named( "special" ), String.class ).to( "special" );
			inPackageOf( List.class ).bind( String.class ).to( "list" );
		}

	}

	private final Injector injector = Bootstrap.injector( InjectronBindsModule.class );

	@Test
	public void thatInjectronIsAvailableForEveryBoundResource() {
		Dependency<? extends Injectron<String>> dependency = dependency( injectronTypeOf( String.class ) );
		Injectron<String> injectron = injector.resolve( dependency );
		assertThat( injectron, notNullValue() );
		assertThat( injectron.instanceFor( dependency( String.class ) ), is( "foobar" ) );
	}

	@Test
	public void thatInjectronArrayIsAvailableForEveryBoundResource() {
		Dependency<? extends Injectron<String>[]> dependency = dependency( injectronsTypeOf( String.class ) );
		Injectron<String>[] injectrons = injector.resolve( dependency );
		assertThat( injectrons.length, is( 3 ) );
	}

	@Test
	public void thatInjectronArrayFiltersByName() {
		Dependency<? extends Injectron<String>[]> dependency = dependency(
				injectronsTypeOf( String.class ) ).named( "special" );
		Injectron<String>[] injectrons = injector.resolve( dependency );
		assertThat( injectrons.length, is( 1 ) );
		assertThat( injectrons[0].instanceFor( dependency( String.class ) ), is( "special" ) );
	}

	@Test
	public void thatInjectorIsAvailableByDefault() {
		Injector resolved = injector.resolve( dependency( Injector.class ) );
		assertThat( resolved, sameInstance( injector ) );
	}
}
