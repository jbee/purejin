package de.jbee.inject;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Name.named;
import static de.jbee.inject.Type.raw;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.LinkedList;
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
		assertResolvedTo( new String[] { "foobar" }, raw( String[].class ) );
		assertResolvedTo( "bar", raw( CharSequence.class ) );
		List<String> list = Arrays.asList( new String[] { "foobar" } );
		assertResolvedTo( list, raw( List.class ).parametized( String.class ) );
		assertResolvedTo( Arrays.asList( new Integer[] { 846, 42 } ),
				raw( List.class ).parametized( Integer.class ) );
		assertResolvedTo( Arrays.asList( new Number[] { 846, 42, 42.0f } ),
				raw( List.class ).parametized( Number.class ).parametizedAsLowerBounds() );
		List<List<String>> listList = new LinkedList<List<String>>();
		listList.add( list );
		assertResolvedTo( listList, raw( List.class ).parametized(
				Type.raw( List.class ).parametized( String.class ) ) );
	}

	@Test
	public void testProvider() {
		Provider<List<String>> p2 = injector.resolve( Dependency.dependency( Type.raw(
				Provider.class ).parametized( Type.raw( List.class ).parametized( String.class ) ) ) );
		System.out.println( p2.yield() );

		Provider<Set<String>> p3 = injector.resolve( Dependency.dependency( Type.raw(
				Provider.class ).parametized( Type.raw( Set.class ).parametized( String.class ) ) ) );
		System.out.println( p3.toString() + " = " + p3.yield() );
	}

	@Test
	public void testMissingFeature() {
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
