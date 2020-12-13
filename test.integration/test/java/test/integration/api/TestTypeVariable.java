package test.integration.api;

import org.junit.jupiter.api.Test;
import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.TypeVariable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.jbee.inject.lang.Type.raw;

/**
 * Tests {@link TypeVariable} utility for formal correctness.
 */
class TestTypeVariable {

	interface Examples<C> {

		<E> List<E> example1();

		<A, B> Function<A, B> example2();

		<T> List<List<T>> example3();

		<E> E[] example4();

		<E> E[][] example5();

		<E> Map<String, List<E[]>> example6();

		<E> List<? extends E> example7();

		<E> Class<E> example8();

		List<C> example9();
	}

	@Test
	void simpleParameterizedType() {
		assertActualType("example1", String.class, "E",
				raw(List.class).parametized(String.class));
	}

	@Test
	void doubleParameterizedType() {
		@SuppressWarnings("rawtypes") Type<Function> actual = raw(Function.class).parametized(String.class,
				Integer.class);
		assertActualType("example2", String.class, "A", actual);
		assertActualType("example2", Integer.class, "B", actual);
	}

	@Test
	void nestedParameterizedType() {
		assertActualType("example3", String.class, "T",
				raw(List.class).parametized(
						raw(List.class).parametized(String.class)));
	}

	@Test
	void oneDimensionalGenericArray() {
		assertActualType("example4", String.class, "E",
				raw(String.class).addArrayDimension());
	}

	@Test
	void twoDimensionalGenericArray() {
		assertActualType("example5", String.class, "E",
				raw(String.class).addArrayDimension().addArrayDimension());
	}

	@Test
	void mapWithParameterizedTypeGenericArray() {
		assertActualType("example6", Float.class, "E",
				raw(Map.class).parametized(raw(String.class),
						raw(List.class).parametized(
								raw(Float.class).addArrayDimension())));
	}

	@Test
	void simpleWildcardType() {
		assertActualType("example7", String.class, "E",
				raw(List.class).parametized(String.class));
	}

	@Test
	void simpleGenericType() {
		assertActualType("example8", String.class, "E",
				raw(Class.class).parametized(String.class));
	}

	@Test
	void simpleClassGenericType() {
		assertActualType("example9", String.class, "C",
				raw(List.class).parametized(String.class));
	}

	private static void assertActualType(String method, Class<?> expected,
			String var, Type<?> actual) {
		assertActualType(method, raw(expected), var, actual);
	}

	private static void assertActualType(String method, Type<?> expected,
			String var, Type<?> actualValue) {
		Map<String, UnaryOperator<Type<?>>> vars = methodReturnTypeVariables(
				method);
		assertTrue(vars.containsKey(var));
		assertEquals(expected, vars.get(var).apply(actualValue));
	}

	private static Map<String, UnaryOperator<Type<?>>> methodReturnTypeVariables(
			String name) {
		try {
			return TypeVariable.typeVariables(Examples.class.getDeclaredMethod(
					name).getGenericReturnType());
		} catch (NoSuchMethodException | SecurityException e) {
			throw new AssertionError(e);
		}
	}
}
