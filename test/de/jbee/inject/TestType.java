package de.jbee.inject;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.Test;

import de.jbee.inject.Type;

public class TestType {

	private List<String> aStringListField;

	@Test
	public void testToString()
			throws Exception {
		Type<List> l = Type.rawType( List.class ).parametizedWith( String.class );
		assertThat( l.toString(), is( "java.util.List<java.lang.String>" ) );

		l = Type.rawType( List.class ).parametizedWith( Type.rawType( String.class ).asLoweBound() );
		assertThat( l.toString(), is( "java.util.List<? extends java.lang.String>" ) );

		l = Type.rawType( List.class ).parametizedWith( Type.rawType( String.class ) ).parametizedAsLowerBounds();
		assertThat( l.toString(), is( "java.util.List<? extends java.lang.String>" ) );

		Field stringList = TestType.class.getDeclaredField( "aStringListField" );
		assertThat( Type.fieldType( stringList ).toString(),
				is( "java.util.List<java.lang.String>" ) );
	}

	@Test
	public void shouldRecognize1DimensionalArrayTypes() {
		Type<Number[]> t = Type.rawType( Number[].class );
		assertTrue( t.isUnidimensionalArray() );
	}

	@Test
	public void shouldNotRecognizeMultiDimensionalArrayTypesAsArray1D() {
		Type<Number[][]> t = Type.rawType( Number[][].class );
		assertFalse( t.isUnidimensionalArray() );
	}

}
