package se.jbee.inject.bind;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.util.Typecast.injectronTypeOf;
import static se.jbee.inject.util.Typecast.injectronsTypeOf;

import java.util.List;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Injectron;
import se.jbee.inject.Name;

public class TestInjectronBinds {

	private static class InjectronBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( String.class ).to( "foobar" );
			Name special = named( "special" );
			bind( special, String.class ).to( "special" );
			inPackageOf( List.class ).bind( String.class ).to( "list" );
			inPackageOf( List.class ).bind( special, String.class ).to( "special-list" );
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
		assertEquals( 4, injectrons.length );
	}

	@Test
	public void thatInjectronArrayIsAvailableForAllResources() {
		assertEquals( 4, injector.resolve( dependency( Injectron[].class ) ).length );
	}

	@Test
	public void thatInjectronArrayFiltersByName() {
		Dependency<? extends Injectron<String>[]> dependency = dependency(
				injectronsTypeOf( String.class ) ).named( "special" );
		Injectron<String>[] injectrons = injector.resolve( dependency );
		assertEquals( 2, injectrons.length );
		assertThat( injectrons[0].instanceFor( dependency( String.class ) ), is( "special-list" ) );
		assertThat( injectrons[1].instanceFor( dependency( String.class ) ), is( "special" ) );
	}

	@Test
	public void thatInjectorIsAvailableByDefault() {
		Injector resolved = injector.resolve( dependency( Injector.class ) );
		assertThat( resolved, sameInstance( injector ) );
	}
}
