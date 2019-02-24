package se.jbee.inject.bind;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Type.raw;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.Parameter;
import se.jbee.inject.bind.Binder.TypedElementBinder;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * Tests that demonstrates how to overlay the default behavior of 1-dimensional
 * array types by defining the elements of them explicitly using the
 * {@link TypedElementBinder#toElements(Parameter, Parameter)} methods.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestElementBinds {

	private static class ElementBindsModule extends BinderModule {

		@Override
		protected void declare() {
			arraybind(String[].class).toElements("foo", "bar");
			arraybind(Number[].class).toElements(2, 3f);
			arraybind(List[].class).toElements(raw(ArrayList.class),
					raw(LinkedList.class));
			bind(ArrayList.class).to(new ArrayList<>());
			bind(LinkedList.class).to(new LinkedList<>());
			arraybind(Float[].class).toElements(2f, 4f, 7f);
			arraybind(Long[].class).toElements(1L, 2L, 3L, 4L); // a varargs
		}
	}

	private final Injector injector = Bootstrap.injector(
			ElementBindsModule.class);

	@Test
	public void thatInstancesAreBoundAsElements() {
		assertArrayEquals(new String[] { "foo", "bar" },
				injector.resolve(String[].class));
	}

	@Test
	public void thatSubtypeInstancesAreBoundAsElements() {
		assertArrayEquals(new Number[] { 2, 3f },
				injector.resolve(Number[].class));
	}

	@Test
	public void thatTypesAreBoundAsElements() {
		List<?>[] elems = injector.resolve(List[].class);
		assertEquals(2, elems.length);
		assertTrue(elems[0] instanceof ArrayList);
		assertTrue(elems[1] instanceof LinkedList);
	}

	@Test
	public void thatConstantsAreBoundAsElements() {
		Float[] floats = injector.resolve(Float[].class);
		assertEquals(2f, floats[0], 0.01f);
		assertEquals(4f, floats[1], 0.01f);
		assertEquals(7f, floats[2], 0.01f);
	}

	@Test
	public void thatVarargsConstantsAreBoundAsElements() {
		assertArrayEquals(new Long[] { 1L, 2L, 3L, 4L },
				injector.resolve(Long[].class));
	}
}
