package test.integration.convert;

import org.junit.jupiter.api.Test;
import se.jbee.inject.convert.Imported;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the {@link Imported} utility.
 */
class TestImported {

	private final Imported imported = Imported.base();

	@Test
	void wildcardType() {
		assertImportedAs("?", "?");
	}

	@Test
	void wildcardGenericType() {
		assertImportedAs("List<? extends Number>",
				"java.util.List<? extends java.lang.Number>");
	}

	@Test
	void wildcardNestedGenericType() {
		assertImportedAs("Function<String, List<? extends Number>>",
				"java.util.function.Function<java.lang.String,java.util.List<? extends java.lang.Number>>");
	}

	@Test
	void oneDimensionalArray() {
		assertImportedAs("Float[]", "java.lang.Float[]");
	}

	@Test
	void twoDimensionalArray() {
		assertImportedAs("Double[][]", "java.lang.Double[][]");
	}

	@Test
	void singleGeneric() {
		assertImportedAs("List<String>", "java.util.List<java.lang.String>");
	}

	@Test
	void singleGenericWithArray() {
		assertImportedAs("List<String[]>", "java.util.List<java.lang.String[]>");
	}

	@Test
	void singleArrayOfGenericWithArray() {
		assertImportedAs("List<String[]>[]",
				"java.util.List<java.lang.String[]>[]");
	}

	@Test
	void singleNestedGeneric() {
		assertImportedAs("List<List<String>>",
				"java.util.List<java.util.List<java.lang.String>>");
	}

	@Test
	void doubleGeneric() {
		assertImportedAs("Function<String,Integer>",
				"java.util.function.Function<java.lang.String,java.lang.Integer>");
	}

	@Test
	void doubleNestedGeneric() {
		assertImportedAs(
				"Function<Function<String,Integer>,Function<String,Integer>>",
				"java.util.function.Function<java.util.function.Function<java.lang.String,java.lang.Integer>,java.util.function.Function<java.lang.String,java.lang.Integer>>");
	}

	@Test
	void trippleGeneric() {
		assertImportedAs("BiFunction<String,Integer,Character>",
				"java.util.function.BiFunction<java.lang.String,java.lang.Integer,java.lang.Character>");
	}

	@Test
	void trippleNestedGeneric() {
		assertImportedAs(
				"BiFunction<String,BiFunction<String,String,Function<Integer,Integer>>,Character>",
				"java.util.function.BiFunction<java.lang.String,java.util.function.BiFunction<java.lang.String,java.lang.String,java.util.function.Function<java.lang.Integer,java.lang.Integer>>,java.lang.Character>");
	}

	@Test
	void errorTooManyTypeArguments() {
		assertError("List<String,Integer>",
				"Expected end of generic type arguments:\n"
					+ "List<String,Integer>\n" + "           ^ here");
	}

	@Test
	void errorTooFewTypeArguments() {
		assertError("Function<String>",
				"Unexpected end of type arguments list:\n"
					+ "Function<String>\n" + "                ^ here");
	}

	private void assertImportedAs(String input, String expected) {
		assertEquals(expected, imported.resolve(input).toString());
	}

	private void assertError(String input, String expectedError) {
		Exception ex = assertThrows(IllegalArgumentException.class,
				() -> imported.resolve(input));
		assertEquals(expectedError, ex.getMessage());
	}
}
