package se.jbee.inject.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import se.jbee.inject.Type;

public class TestTypecast {

	@Test
	public void thatReturnTypeConformsToModelledType() {
		Type<java.util.List<java.lang.String>> listString = Typecast.listTypeOf( String.class );
		assertEquals( "java.util.List<java.lang.String>", listString.toString() );
	}

	@Test
	public void thatReturnTypeConformsToNestedModelledType() {
		Type<java.util.List<java.util.List<java.lang.String>>> listListString = Typecast.listTypeOf( Typecast.listTypeOf( String.class ) );
		assertEquals( "java.util.List<java.util.List<java.lang.String>>", listListString.toString() );
	}
}
