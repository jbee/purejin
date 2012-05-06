package de.jbee.inject;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.Test;

public class TestType {

	private List<String> aStringListField;

	@Test
	public void testToString()
			throws Exception {
		Type<List> l = Type.rawType( List.class ).parametizedWith( String.class );
		assertThat( l.toString(), is( "java.util.List<java.lang.String>" ) );

		l = Type.rawType( List.class ).parametizedWith( Type.rawType( String.class ).asLowerBound() );
		assertThat( l.toString(), is( "java.util.List<? extends java.lang.String>" ) );

		l = Type.rawType( List.class ).parametizedWith( Type.rawType( String.class ) ).parametizedAsLowerBounds();
		assertThat( l.toString(), is( "java.util.List<? extends java.lang.String>" ) );

		Field stringList = TestType.class.getDeclaredField( "aStringListField" );
		assertThat( Type.fieldType( stringList ).toString(),
				is( "java.util.List<java.lang.String>" ) );
	}

	@Test
	public void testIsAssignableTo() {
		Type<Integer> integer = Type.rawType( Integer.class );
		Type<Number> number = Type.rawType( Number.class );
		assertTrue( integer.isAssignableTo( number ) );
		assertFalse( number.isAssignableTo( integer ) );
	}

	@Test
	public void testIsAssignableTo1Generic() {
		Type<List> listOfIntegers = Type.rawType( List.class ).parametizedWith( Integer.class );
		Type<List> listOfNumbers = Type.rawType( List.class ).parametizedWith( Number.class );
		assertFalse( listOfIntegers.isAssignableTo( listOfNumbers ) );
		assertFalse( listOfNumbers.isAssignableTo( listOfIntegers ) );
		assertTrue( listOfIntegers.isAssignableTo( listOfNumbers.parametizedAsLowerBounds() ) );
		assertFalse( listOfNumbers.isAssignableTo( listOfIntegers.parametizedAsLowerBounds() ) );
	}

	@Test
	public void testIsAssignableTo2Generics() {
		List<Class<Integer>> l = null;
		List<? extends Class<? extends Number>> l2 = l;

		Type<List> listOfClassesOfIntegers = Type.rawType( List.class ).parametizedWith(
				Type.rawType( Class.class ).parametizedWith( Integer.class ) );
		Type<Class> classOfNumbers = Type.rawType( Class.class ).parametizedWith( Number.class );
		Type<List> listOfClassesOfNumbers = Type.rawType( List.class ).parametizedWith(
				classOfNumbers );
		assertFalse( listOfClassesOfIntegers.isAssignableTo( listOfClassesOfNumbers ) );
		assertFalse( listOfClassesOfIntegers.isAssignableTo( listOfClassesOfNumbers.parametizedAsLowerBounds() ) );
		Type<List> listOfExtendsClassesOfExtendsNumbers = listOfClassesOfNumbers.parametizedWith( classOfNumbers.asLowerBound().parametizedAsLowerBounds() );
		assertTrue( listOfClassesOfIntegers.isAssignableTo( listOfExtendsClassesOfExtendsNumbers ) );
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
