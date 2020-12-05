package test.integration.convert;

import org.junit.jupiter.api.Test;
import se.jbee.inject.convert.Imported;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests the {@link Imported} utility.
 *
 * @author Jan Bernitt
 */
public class TestImported {

	private final Imported imported = Imported.base();

	@Test
	public void wildcardType() {
		assertResolved("?", "?");
	}

	@Test
	public void wildcardGenericType() {
		assertResolved("List<? extends Number>",
				"java.util.List<? extends java.lang.Number>");
	}

	@Test
	public void wildcardNestedGenericType() {
		assertResolved("Function<String, List<? extends Number>>",
				"java.util.function.Function<java.lang.String,java.util.List<? extends java.lang.Number>>");
	}

	@Test
	public void oneDimensionalArray() {
		assertResolved("Float[]", "java.lang.Float[]");
	}

	@Test
	public void twoDimensionalArray() {
		assertResolved("Double[][]", "java.lang.Double[][]");
	}

	@Test
	public void singleGeneric() {
		assertResolved("List<String>", "java.util.List<java.lang.String>");
	}

	@Test
	public void singleGenericWithArray() {
		assertResolved("List<String[]>", "java.util.List<java.lang.String[]>");
	}

	@Test
	public void singleArrayOfGenericWithArray() {
		assertResolved("List<String[]>[]",
				"java.util.List<java.lang.String[]>[]");
	}

	@Test
	public void singleNestedGeneric() {
		assertResolved("List<List<String>>",
				"java.util.List<java.util.List<java.lang.String>>");
	}

	@Test
	public void doubleGeneric() {
		assertResolved("Function<String,Integer>",
				"java.util.function.Function<java.lang.String,java.lang.Integer>");
	}

	@Test
	public void doubleNestedGeneric() {
		assertResolved(
				"Function<Function<String,Integer>,Function<String,Integer>>",
				"java.util.function.Function<java.util.function.Function<java.lang.String,java.lang.Integer>,java.util.function.Function<java.lang.String,java.lang.Integer>>");
	}

	@Test
	public void trippleGeneric() {
		assertResolved("BiFunction<String,Integer,Character>",
				"java.util.function.BiFunction<java.lang.String,java.lang.Integer,java.lang.Character>");
	}

	@Test
	public void trippleNestedGeneric() {
		assertResolved(
				"BiFunction<String,BiFunction<String,String,Function<Integer,Integer>>,Character>",
				"java.util.function.BiFunction<java.lang.String,java.util.function.BiFunction<java.lang.String,java.lang.String,java.util.function.Function<java.lang.Integer,java.lang.Integer>>,java.lang.Character>");
	}

	@Test
	public void errorTooManyTypeArguments() {
		assertError("List<String,Integer>",
				"Expected end of generic type arguments:\n"
					+ "List<String,Integer>\n" + "           ^ here");
	}

	@Test
	public void errorTooFewTypeArguments() {
		assertError("Function<String>",
				"Unexpected end of type arguments list:\n"
					+ "Function<String>\n" + "                ^ here");
	}

	private void assertResolved(String input, String expected) {
		assertEquals(expected, imported.resolve(input).toString());
	}

	private void assertError(String input, String expectedError) {
		try {
			imported.resolve(input);
			fail("Expected error for input: " + input);
		} catch (IllegalArgumentException e) {
			assertEquals(expectedError, e.getMessage());
		}
	}
}
