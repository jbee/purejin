package de.jbee.inject;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class TestType {

	private List<String> aStringListField;

	@Test
	public void testToString()
			throws Exception {
		Type<List> l = Type.raw( List.class ).parametized( String.class );
		assertThat( l.toString(), is( "java.util.List<java.lang.String>" ) );

		l = Type.raw( List.class ).parametized( Type.raw( String.class ).asLowerBound() );
		assertThat( l.toString(), is( "java.util.List<? extends java.lang.String>" ) );

		l = Type.raw( List.class ).parametized( Type.raw( String.class ) ).parametizedAsLowerBounds();
		assertThat( l.toString(), is( "java.util.List<? extends java.lang.String>" ) );

		Field stringList = TestType.class.getDeclaredField( "aStringListField" );
		assertThat( Type.fieldType( stringList ).toString(),
				is( "java.util.List<java.lang.String>" ) );
	}

	@Test
	public void testIsAssignableTo() {
		Type<Integer> integer = Type.raw( Integer.class );
		Type<Number> number = Type.raw( Number.class );
		assertTrue( integer.isAssignableTo( number ) );
		assertFalse( number.isAssignableTo( integer ) );
	}

	@Test
	public void testIsAssignableTo1Generic() {
		Type<List> listOfIntegers = Type.raw( List.class ).parametized( Integer.class );
		Type<List> listOfNumbers = Type.raw( List.class ).parametized( Number.class );
		assertFalse( listOfIntegers.isAssignableTo( listOfNumbers ) );
		assertFalse( listOfNumbers.isAssignableTo( listOfIntegers ) );
		assertTrue( listOfIntegers.isAssignableTo( listOfNumbers.parametizedAsLowerBounds() ) );
		assertFalse( listOfNumbers.isAssignableTo( listOfIntegers.parametizedAsLowerBounds() ) );
	}

	@Test
	public void testIsAssignableTo2Generics() {
		List<Class<Integer>> l = null;
		List<? extends Class<? extends Number>> l2 = l;

		Type<List> listOfClassesOfIntegers = Type.raw( List.class ).parametized(
				Type.raw( Class.class ).parametized( Integer.class ) );
		Type<Class> classOfNumbers = Type.raw( Class.class ).parametized( Number.class );
		Type<List> listOfClassesOfNumbers = Type.raw( List.class ).parametized( classOfNumbers );
		assertFalse( listOfClassesOfIntegers.isAssignableTo( listOfClassesOfNumbers ) );
		assertFalse( listOfClassesOfIntegers.isAssignableTo( listOfClassesOfNumbers.parametizedAsLowerBounds() ) );
		Type<List> listOfExtendsClassesOfExtendsNumbers = listOfClassesOfNumbers.parametized( classOfNumbers.asLowerBound().parametizedAsLowerBounds() );
		assertTrue( listOfClassesOfIntegers.isAssignableTo( listOfExtendsClassesOfExtendsNumbers ) );
	}

	@Test
	public void testGenericArrays() {
		Type<Class[]> classArray = Type.raw( Class[].class ).parametized( String.class );
		assertThat( classArray.elementType().toString(), is( "java.lang.Class<java.lang.String>" ) );
	}

	@Test
	public void shouldRecognize1DimensionalArrayTypes() {
		Type<Number[]> t = Type.raw( Number[].class );
		assertTrue( t.isUnidimensionalArray() );
	}

	@Test
	public void shouldNotRecognizeMultiDimensionalArrayTypesAsArray1D() {
		Type<Number[][]> t = Type.raw( Number[][].class );
		assertFalse( t.isUnidimensionalArray() );
	}

	@Test
	public void testMorePreciseThan() {
		Type<Integer> integer = Type.raw( Integer.class );
		Type<Number> number = Type.raw( Number.class );
		assertMorePrecise( integer, number );
	}

	@Test
	public void testMorePreciseThanLowerBound() {
		Type<Integer> integer = Type.raw( Integer.class );
		Type<? extends Integer> integerLower = Type.raw( Integer.class ).asLowerBound();
		assertMorePrecise( integer, integerLower );
	}

	@Test
	public void testMorePreciseThanIndependend() {
		Type<Integer> integer = Type.raw( Integer.class );
		Type<String> string = Type.raw( String.class );
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
		Type<List> integer = Type.raw( List.class ).parametized( Integer.class );
		Type<List> number = Type.raw( List.class ).parametized( Number.class );
		assertMorePrecise( integer, number );
	}

	@Test
	public void testMorePreciseThanWithGenericsAndWithout() {
		Type<List> integer = Type.raw( List.class ).parametized( Integer.class );
		Type<List> wildcardList = Type.raw( List.class );
		assertMorePrecise( integer, wildcardList );
	}

	@Test
	public void testMorePreciseThanWithWildcardGenerics() {
		Type<List> integer = Type.raw( List.class ).parametized( Integer.class );
		Type<List> wildcardInteger = Type.raw( List.class ).parametized( Integer.class ).parametizedAsLowerBounds();
		assertMorePrecise( integer, wildcardInteger );
	}

	@Test
	public void testMorePreciseThanWithSubclassAndGenerics() {
		Type<ArrayList> arrayListOfIntegers = Type.raw( ArrayList.class ).parametized(
				Integer.class );
		Type<List> listOfIntegers = Type.raw( List.class ).parametized( Integer.class );
		assertMorePrecise( arrayListOfIntegers, listOfIntegers );
		//TODO this actually works because List and ArrayList just have the same geneic but if the subclass uses another index we will compare the wrong thing right now
	}

	@Test
	public void testArrayType() {
		Type<? extends Number> t = Type.raw( Number.class ).asLowerBound();
		assertThat( t.getArrayType().toString(), is( "? extends java.lang.Number[]" ) );
	}

	@Test
	public void thatGenericSuperInterfacesAreAvailableAsSupertype() {
		Type<? super Integer>[] intergerSupertypes = Type.raw( Integer.class ).supertypes();
		assertContains( intergerSupertypes,
				Type.raw( Comparable.class ).parametized( Integer.class ) );
	}

	private void assertContains( Type<?>[] actual, Type<?> expected ) {
		for ( Type<?> type : actual ) {
			if ( type.equalTo( expected ) ) {
				return;
			}
		}
		fail( Arrays.toString( actual ) + " should have contained: " + expected );
	}
}
