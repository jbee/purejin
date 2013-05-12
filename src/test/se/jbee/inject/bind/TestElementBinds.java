package se.jbee.inject.bind;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Type.raw;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bind.Binder.TypedElementBinder;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * Tests that demonstrates how to overlay the default behavior of 1-dimensional array types by
 * defining the elements of them explicitly using the
 * {@link TypedElementBinder#toElements(Class, Class)} methods.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestElementBinds {

	private static class ElementBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			arraybind( String[].class ).toElements( "foo", "bar" );
			arraybind( Number[].class ).toElements( 2, 3f );
			arraybind( List[].class ).toElements( raw( ArrayList.class ), raw( LinkedList.class ) );
			bind( ArrayList.class ).to( new ArrayList<Object>() );
			bind( LinkedList.class ).to( new LinkedList<Object>() );
			arraybind( Float[].class ).toElements( 2f, 4f, 7f );
		}
	}

	private final Injector injector = Bootstrap.injector( ElementBindsModule.class );

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

	@Test
	public void thatConstantsAreBoundAsElements() {
		Float[] floats = injector.resolve( dependency( Float[].class ) );
		assertEquals( 2f, floats[0].floatValue(), 0.01f );
		assertEquals( 4f, floats[1].floatValue(), 0.01f );
		assertEquals( 7f, floats[2].floatValue(), 0.01f );
	}
}
