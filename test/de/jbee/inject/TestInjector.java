package de.jbee.inject;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Name.named;
import static de.jbee.inject.Type.raw;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import de.jbee.inject.util.PackageModule;

public class TestInjector {

	static class TestModule
			extends PackageModule {

		@Override
		protected void configure() {
			install( Module.BUILD_IN );
			bind( String.class ).to( "foobar" );
			bind( CharSequence.class ).to( "bar" );
			bind( Integer.class ).to( 42 );
			bind( named( "foo" ), Integer.class ).to( 846 );
			bind( Float.class ).to( 42.0f );
		}

	}

	private final Injector injector = Injector.create( new TestModule(), new BuildinModuleBinder() );

	@Test
	public void buildinProviderShouldBeAvailableForAnyBoundType() {
		assertProviderResolvedTo( "foobar", raw( String.class ) );
		assertProviderResolvedTo( 846, raw( Integer.class ) );
	}

	@Test
	public void buildinProviderShouldBeAvailableForAnyNamedBoundType() {
		assertProviderResolvedTo( 846, raw( Integer.class ), named( "foo" ) );
	}

	@Test
	public void test() {

		System.out.println( Arrays.toString( injector.resolve( Dependency.dependency( Type.raw( String[].class ) ) ) ) );
		System.out.println( injector.resolve( Dependency.dependency( Type.raw( CharSequence.class ) ) ) );
		System.out.println( injector.resolve( Dependency.dependency( Type.raw( List.class ).parametized(
				String.class ) ) ) );
		Provider<List<String>> p2 = injector.resolve( Dependency.dependency( Type.raw(
				Provider.class ).parametized( Type.raw( List.class ).parametized( String.class ) ) ) );
		System.out.println( p2.yield() );

		Provider<Set<String>> p3 = injector.resolve( Dependency.dependency( Type.raw(
				Provider.class ).parametized( Type.raw( Set.class ).parametized( String.class ) ) ) );
		System.out.println( p3.toString() + " = " + p3.yield() );

		System.out.println( injector.resolve( Dependency.dependency( Type.raw( List.class ).parametized(
				Integer.class ) ) ) );

		System.out.println( injector.resolve( Dependency.dependency( Type.raw( List.class ).parametized(
				Number.class ).parametizedAsLowerBounds() ) ) );

		List<List<String>> lls = injector.resolve( Dependency.dependency( Type.raw( List.class ).parametized(
				Type.raw( List.class ).parametized( String.class ) ) ) );
		System.out.println( lls );

		System.out.println( injector.resolve( Dependency.dependency( Type.raw( Integer.class ) ).named(
				"foo" ) ) );

		injector.resolve( Dependency.dependency( Type.raw( Number.class ).asLowerBound() ).named(
				"foo" ) );
	}

	private <T> void assertProviderResolvedTo( T expected, Type<? extends T> dependencyType ) {
		assertProviderResolvedTo( expected, dependencyType, Name.ANY );
	}

	private <T> void assertProviderResolvedTo( T expected, Type<? extends T> dependencyType,
			Name name ) {
		Type<Provider> type = raw( Provider.class ).parametized( dependencyType );
		Provider<?> provider = injector.resolve( dependency( type ).named( name ) );
		assertEquals( provider.yield(), expected );
	}

	private <T> void assertResolvedTo( T expected, Type<? extends T> dependencyType ) {
		assertThat( injector.resolve( dependency( dependencyType ) ), is( expected ) );
	}
}
