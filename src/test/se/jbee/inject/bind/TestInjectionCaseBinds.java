package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.container.Typecast.injectionCaseTypeFor;
import static se.jbee.inject.container.Typecast.injectionCasesTypeFor;

import java.util.List;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.InjectionCase;
import se.jbee.inject.bootstrap.Bootstrap;

public class TestInjectionCaseBinds {

	private static class TestInjectionCaseBindsModule
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

	private final Injector injector = Bootstrap.injector( TestInjectionCaseBindsModule.class );

	@Test
	public void thatInjectionCaseIsAvailableForEveryBoundResource() {
		InjectionCase<String> icase = injector.resolve( injectionCaseTypeFor( String.class ) );
		assertNotNull( icase );
		assertEquals( "foobar", icase.generator.instanceFor( dependency( String.class ) ) );
	}

	@Test
	public void thatInjectionCaseArrayIsAvailableForEveryBoundResource() {
		InjectionCase<String>[] icase = injector.resolve( injectionCasesTypeFor( String.class ) );
		assertEquals( 4, icase.length );
	}

	@Test
	public void thatInjectionCaseArrayIsAvailableForAllResources() {
		assertEquals( 4, injector.resolve( InjectionCase[].class ).length );
	}

	@Test
	public void thatInjectionCaseArrayFiltersByName() {
		Dependency<? extends InjectionCase<String>[]> dependency = dependency(
				injectionCasesTypeFor( String.class ) ).named( "special" );
		InjectionCase<String>[] cases = injector.resolve( dependency );
		assertEquals( 2, cases.length );
		assertEquals( "special-list", cases[0].generator.instanceFor( dependency( String.class ) ) );
		assertEquals( "special", cases[1].generator.instanceFor( dependency( String.class ) ) );
	}

	@Test
	public void thatInjectorIsAvailableByDefault() {
		assertSame( injector, injector.resolve( Injector.class ) );
	}
}
