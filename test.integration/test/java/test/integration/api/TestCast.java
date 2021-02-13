package test.integration.api;

import org.junit.jupiter.api.Test;
import se.jbee.lang.Cast;
import se.jbee.lang.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestCast {

	@Test
	void thatReturnTypeConformsToModelledType() {
		Type<java.util.List<java.lang.String>> listString = Cast.listTypeOf(
				String.class);
		assertEquals("java.util.List<java.lang.String>", listString.toString());
	}

	@Test
	void thatReturnTypeConformsToNestedModelledType() {
		Type<java.util.List<java.util.List<java.lang.String>>> listListString = Cast.listTypeOf(
				Cast.listTypeOf(String.class));
		assertEquals("java.util.List<java.util.List<java.lang.String>>",
				listListString.toString());
	}
}
