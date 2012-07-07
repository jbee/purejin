package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Name.named;
import static de.jbee.inject.Type.raw;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import de.jbee.inject.Dependency;
import de.jbee.inject.DependencyResolver;
import de.jbee.inject.Injectron;
import de.jbee.inject.Name;
import de.jbee.inject.Provider;
import de.jbee.inject.Type;

public class TestInstanceBinds {

	private static class InstanceBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( String.class ).to( "foobar" );
			bind( CharSequence.class ).to( "bar" );
			bind( Integer.class ).to( 42 );
			bind( named( "foo" ), Integer.class ).to( 846 );
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

	private final DependencyResolver injector = Bootstrap.injector( InstanceBindsBundle.class );

	@Test
	public void buildinProviderShouldBeAvailableForAnyBoundType() {
		assertInjectsProviderFor( "foobar", raw( String.class ) );
		assertInjectsProviderFor( 42, raw( Integer.class ) );
	}

	@Test
	public void buildinProviderShouldBeAvailableForAnyNamedBoundType() {
		assertInjectsProviderFor( 42, raw( Integer.class ), named( "foo" ) );
	}

	@Test
	public void test() {
		assertInjects( new String[] { "foobar" }, raw( String[].class ) );
		assertInjects( "bar", raw( CharSequence.class ) );
		List<String> list = singletonList( "foobar" );
		assertInjects( list, raw( List.class ).parametized( String.class ) );
		assertInjects( Arrays.asList( new Integer[] { 42, 846 } ), raw( List.class ).parametized(
				Integer.class ) );
		assertInjectsItems( Arrays.asList( new Number[] { 846, 42, 42.0f } ),
				raw( List.class ).parametized( Number.class ).parametizedAsLowerBounds() );
		assertInjects( singletonList( list ), raw( List.class ).parametized(
				raw( List.class ).parametized( String.class ) ) );
	}

	@Test
	public void testProvider() {
		List<String> list = singletonList( "foobar" );
		assertInjectsProviderFor( list, raw( List.class ).parametized( String.class ) );
		assertInjectsProviderFor( singleton( "foobar" ),
				raw( Set.class ).parametized( String.class ) );
	}

	@Test
	public void testInjectron() {
		Dependency<Injectron> dependency = dependency( raw( Injectron.class ).parametized(
				String.class ) );
		Injectron<String> injectron = injector.resolve( dependency );
		assertThat( injectron, notNullValue() );
		assertThat( injectron.instanceFor( dependency( String.class ) ), is( "foobar" ) );
	}

	@Test
	@Ignore
	public void testMissingFeature() {
		injector.resolve( Dependency.dependency( Type.raw( Number.class ).asLowerBound() ).named(
				"foo" ) );
	}

	private <T> void assertInjectsProviderFor( T expected, Type<? extends T> dependencyType ) {
		assertInjectsProviderFor( expected, dependencyType, Name.ANY );
	}

	private <T> void assertInjectsProviderFor( T expected, Type<? extends T> dependencyType,
			Name name ) {
		Type<Provider> type = raw( Provider.class ).parametized( dependencyType );
		Provider<?> provider = injector.resolve( dependency( type ).named( name ) );
		assertEquals( provider.provide(), expected );
	}

	private <T> void assertInjects( T expected, Type<? extends T> dependencyType ) {
		assertThat( injector.resolve( dependency( dependencyType ) ), is( expected ) );
	}

	@SuppressWarnings ( "unchecked" )
	private <E> void assertInjectsItems( List<E> expected, Type<? extends List> dependencyType ) {
		assertTrue( injector.resolve( dependency( dependencyType ) ).containsAll( expected ) );
	}
}
