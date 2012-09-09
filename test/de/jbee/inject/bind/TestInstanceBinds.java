package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Name.named;
import static de.jbee.inject.Type.listTypeOf;
import static de.jbee.inject.Type.raw;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

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

	private final Injector injector = Bootstrap.injector( InstanceBindsBundle.class );

	@Test
	public void thatInstanceInjectedBasedOnTheDependencyType() {
		assertInjects( "bar", raw( CharSequence.class ) );
	}

	@Test
	public void thatArrayTypeIsAvailableForAnyBoundType() {
		assertInjects( new String[] { "foobar" }, raw( String[].class ) );
	}

	@Test
	public void testListIsAvailableForBoundType() {
		assertInjects( singletonList( "foobar" ), listTypeOf( String.class ) );
		assertInjects( asList( new Integer[] { 42, 846 } ), listTypeOf( Integer.class ) );
	}

	@Test
	public void thatListAsLowerBoundIsAvailable() {
		Type<? extends List<Number>> wildcardListType = listTypeOf( Number.class ).parametizedAsLowerBounds();
		assertInjectsItems( new Number[] { 846, 42, 42.0f }, wildcardListType );
	}

	@Test
	public void thatListOfListsOfBoundTypesAreAvailable() {
		assertInjects( singletonList( singletonList( "foobar" ) ),
				listTypeOf( listTypeOf( String.class ) ) );
	}

	@Test
	@Ignore
	public void thatLowerBoundsCanBeUsedToGetAnAvailableResource() {
		injector.resolve( dependency( Type.raw( Number.class ).asLowerBound() ).named( "foo" ) );
	}

	private <T> void assertInjects( T expected, Type<? extends T> dependencyType ) {
		assertThat( injector.resolve( dependency( dependencyType ) ), is( expected ) );
	}

	private <E> void assertInjectsItems( E[] expected, Type<? extends List<?>> dependencyType ) {
		assertInjectsItems( asList( expected ), dependencyType );
	}

	@SuppressWarnings ( "unchecked" )
	private <E> void assertInjectsItems( List<E> expected, Type<? extends List<?>> dependencyType ) {
		assertTrue( injector.resolve( dependency( dependencyType ) ).containsAll( expected ) );
	}

}
