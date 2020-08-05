package test.integration.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import se.jbee.inject.Cast;
import se.jbee.inject.lang.Type;

public class TestCast {

	@Test
	public void thatReturnTypeConformsToModelledType() {
		Type<java.util.List<java.lang.String>> listString = Cast.listTypeOf(
				String.class);
		assertEquals("java.util.List<java.lang.String>", listString.toString());
	}

	@Test
	public void thatReturnTypeConformsToNestedModelledType() {
		Type<java.util.List<java.util.List<java.lang.String>>> listListString = Cast.listTypeOf(
				Cast.listTypeOf(String.class));
		assertEquals("java.util.List<java.util.List<java.lang.String>>",
				listListString.toString());
	}
}
