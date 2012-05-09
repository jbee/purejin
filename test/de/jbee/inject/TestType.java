package de.jbee.inject;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

public class TestType {

	List<String> aStringListField;

	@Test
	public void testToString()
			throws Exception {
		Type<List> l = Type.rawType( List.class ).parametized( String.class );
		assertThat( l.toString(), is( "java.util.List<java.lang.String>" ) );

		l = Type.rawType( List.class ).parametized( Type.rawType( String.class ).asLowerBound() );
		assertThat( l.toString(), is( "java.util.List<? extends java.lang.String>" ) );

		l = Type.rawType( List.class ).parametized( Type.rawType( String.class ) ).parametizedAsLowerBounds();
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
		Type<List> listOfIntegers = Type.rawType( List.class ).parametized( Integer.class );
		Type<List> listOfNumbers = Type.rawType( List.class ).parametized( Number.class );
		assertFalse( listOfIntegers.isAssignableTo( listOfNumbers ) );
		assertFalse( listOfNumbers.isAssignableTo( listOfIntegers ) );
		assertTrue( listOfIntegers.isAssignableTo( listOfNumbers.parametizedAsLowerBounds() ) );
		assertFalse( listOfNumbers.isAssignableTo( listOfIntegers.parametizedAsLowerBounds() ) );
	}

	@Test
	public void testIsAssignableTo2Generics() {
		List<Class<Integer>> l = null;
		List<? extends Class<? extends Number>> l2 = l;

		Type<List> listOfClassesOfIntegers = Type.rawType( List.class ).parametized(
				Type.rawType( Class.class ).parametized( Integer.class ) );
		Type<Class> classOfNumbers = Type.rawType( Class.class ).parametized( Number.class );
		Type<List> listOfClassesOfNumbers = Type.rawType( List.class ).parametized( classOfNumbers );
		assertFalse( listOfClassesOfIntegers.isAssignableTo( listOfClassesOfNumbers ) );
		assertFalse( listOfClassesOfIntegers.isAssignableTo( listOfClassesOfNumbers.parametizedAsLowerBounds() ) );
		Type<List> listOfExtendsClassesOfExtendsNumbers = listOfClassesOfNumbers.parametized( classOfNumbers.asLowerBound().parametizedAsLowerBounds() );
		assertTrue( listOfClassesOfIntegers.isAssignableTo( listOfExtendsClassesOfExtendsNumbers ) );
	}

	@Test
	public void testGenericArrays() {
		Type<Class[]> classArray = Type.rawType( Class[].class ).parametized( String.class );
		assertThat( classArray.getElementType().toString(),
				is( "java.lang.Class<java.lang.String>" ) );
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

	@Test
	public void testMorePreciseThan() {
		Type<Integer> integer = Type.rawType( Integer.class );
		Type<Number> number = Type.rawType( Number.class );
		assertMorePrecise( integer, number );
	}

	@Test
	public void testMorePreciseThanLowerBound() {
		Type<Integer> integer = Type.rawType( Integer.class );
		Type<? extends Integer> integerLower = Type.rawType( Integer.class ).asLowerBound();
		assertMorePrecise( integer, integerLower );
	}

	@Test
	public void testMorePreciseThanIndependend() {
		Type<Integer> integer = Type.rawType( Integer.class );
		Type<String> string = Type.rawType( String.class );
		assertTrue( integer.morePreciseThan( string ) );
		assertTrue( string.morePreciseThan( integer ) );
		//OPEN maybe this should eval to false both times ?
	}

	private static void assertMorePrecise( Type<?> morePrecise, Type<?> lessPrecise ) {
		assertTrue( morePrecise.morePreciseThan( lessPrecise ) );
		assertFalse( lessPrecise.morePreciseThan( morePrecise ) );
	}

	@Test
	public void testMorePreciseThanWithGenerics() {
		Type<List> integer = Type.rawType( List.class ).parametized( Integer.class );
		Type<List> number = Type.rawType( List.class ).parametized( Number.class );
		assertMorePrecise( integer, number );
	}

	@Test
	public void testMorePreciseThanWithGenericsAndWithout() {
		Type<List> integer = Type.rawType( List.class ).parametized( Integer.class );
		Type<List> wildcardList = Type.rawType( List.class );
		assertMorePrecise( integer, wildcardList );
	}

	@Test
	public void testMorePreciseThanWithWildcardGenerics() {
		Type<List> integer = Type.rawType( List.class ).parametized( Integer.class );
		Type<List> wildcardInteger = Type.rawType( List.class ).parametized( Integer.class ).parametizedAsLowerBounds();
		assertMorePrecise( integer, wildcardInteger );
	}

	@Test
	public void testMorePreciseThanWithSubclassAndGenerics() {
		Type<ArrayList> arrayListOfIntegers = Type.rawType( ArrayList.class ).parametized(
				Integer.class );
		Type<List> listOfIntegers = Type.rawType( List.class ).parametized( Integer.class );
		assertMorePrecise( arrayListOfIntegers, listOfIntegers );
		//TODO this actually works because List and ArrayList just have the same geneic but if the subclass uses another index we will compare the wrong thing right now
	}

	@Test
	@Ignore
	public void testAsSupertype() {
		Type<ArrayList> listOfStrings = Type.rawType( ArrayList.class ).parametized( String.class );
		assertThat( listOfStrings.asSupertype( List.class ).toString(),
				is( "java.util.List<java.lang.String>" ) );
	}
}
