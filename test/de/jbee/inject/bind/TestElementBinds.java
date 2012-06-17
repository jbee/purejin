package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import de.jbee.inject.DependencyResolver;

public class TestElementBinds {

	static class ElementBindsModule
			extends PackageModule {

		@Override
		protected void configure() {
			bind( String[].class ).toElements( "foo", "bar" );
			bind( Number[].class ).toElements( 2, 3f );
			bind( List[].class ).toElements( ArrayList.class, LinkedList.class );
		}
	}

	private final DependencyResolver injector = Bootstrap.injector( ElementBindsModule.class );

	@Test
	public void thatInstancesAreBoundAsElements() {
		assertArrayEquals( injector.resolve( dependency( String[].class ) ), new String[] { "foo",
				"bar" } );
	}

	@Test
	public void thatSubtypeInstancesAreBoundAsElements() {
		assertArrayEquals( injector.resolve( dependency( Number[].class ) ), new Number[] { 2, 3f } );
	}

	@Test
	public void thatTypesAreBoundAsElements() {
		List<?>[] elems = injector.resolve( dependency( List[].class ) );
		assertThat( elems.length, is( 2 ) );
		assertThat( elems[0], instanceOf( ArrayList.class ) );
		assertThat( elems[1], instanceOf( LinkedList.class ) );
	}
}
