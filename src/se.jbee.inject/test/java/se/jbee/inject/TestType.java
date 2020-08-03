package se.jbee.inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static se.jbee.inject.Type.classType;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Type.returnType;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

@SuppressWarnings({ "rawtypes" })
public class TestType {

	private interface Baz<T> {
		// needed to check supertypes() method
	}

	private static class Bar<A, B> extends Foo<B, String> implements Baz<A> {
		// needed to check supertypes() method
	}

	private static class Foo<A, B> extends Qux<A> {
		// needed to check supertypes() method
	}

	private static class Qux<R> implements QuxQux<R> {
		// needed to check supertypes() method
	}

	private interface QuxQux<T> {
		// needed to check supertypes() method
	}

	private interface XList<X extends Serializable, E> extends List<E> {
		// needed to check supertypes() and isAssignableTo methods
	}

	public <E> XList<String, E> typeVariableWithActualTypeArgument() {
		return null; // needed to check actual type arguments
	}

	public <E extends Number> XList<E, Integer> typeVariableWithActualTypeArgument2() {
		return null; // needed to check actual type arguments
	}

	/**
	 * Reflection!!!
	 */
	@SuppressWarnings("unused")
	private List<String> aStringListField;

	@Test
	public void testToString() throws Exception {
		Type<List> l = Type.raw(List.class).parametized(String.class);
		assertEquals("java.util.List<java.lang.String>", l.toString());

		l = Type.raw(List.class).parametized(
				Type.raw(String.class).asUpperBound());
		assertEquals("java.util.List<? extends java.lang.String>",
				l.toString());

		l = Type.raw(List.class).parametized(
				Type.raw(String.class)).parametizedAsUpperBounds();
		assertEquals("java.util.List<? extends java.lang.String>",
				l.toString());

		Field stringList = TestType.class.getDeclaredField("aStringListField");
		assertEquals("java.util.List<java.lang.String>",
				Type.fieldType(stringList).toString());
	}

	@Test
	public void testIsAssignableTo() {
		Type<Integer> integer = Type.raw(Integer.class);
		Type<Number> number = Type.raw(Number.class);
		assertTrue(integer.isAssignableTo(number));
		assertFalse(number.isAssignableTo(integer));
	}

	@Test
	public void testIsAssignableTo1Generic() {
		Type<List> listOfIntegers = Type.raw(List.class).parametized(
				Integer.class);
		Type<List> listOfNumbers = Type.raw(List.class).parametized(
				Number.class);
		assertFalse(listOfIntegers.isAssignableTo(listOfNumbers));
		assertFalse(listOfNumbers.isAssignableTo(listOfIntegers));
		assertTrue(listOfIntegers.isAssignableTo(
				listOfNumbers.parametizedAsUpperBounds()));
		assertFalse(listOfNumbers.isAssignableTo(
				listOfIntegers.parametizedAsUpperBounds()));
	}

	@Test
	public void testIsAssignableTo2Generics() {
		Type<List> listOfClassesOfIntegers = Type.raw(List.class).parametized(
				Type.raw(Class.class).parametized(Integer.class));
		Type<Class> classOfNumbers = Type.raw(Class.class).parametized(
				Number.class);
		Type<List> listOfClassesOfNumbers = Type.raw(List.class).parametized(
				classOfNumbers);
		assertFalse(
				listOfClassesOfIntegers.isAssignableTo(listOfClassesOfNumbers));
		assertFalse(listOfClassesOfIntegers.isAssignableTo(
				listOfClassesOfNumbers.parametizedAsUpperBounds()));
		Type<List> listOfExtendsClassesOfExtendsNumbers = listOfClassesOfNumbers.parametized(
				classOfNumbers.asUpperBound().parametizedAsUpperBounds());
		assertTrue(listOfClassesOfIntegers.isAssignableTo(
				listOfExtendsClassesOfExtendsNumbers));
	}

	@Test
	public void testGenericArrays() {
		Type<Class[]> classArray = Type.raw(Class[].class).parametized(
				String.class);
		assertEquals("java.lang.Class<java.lang.String>",
				classArray.baseType().toString());
		assertEquals("java.lang.Class<java.lang.String>[]",
				classArray.toString());
	}

	@Test
	public void testTwoDimensionalGenericArrays() {
		Type<Class[][]> classArray = Type.raw(Class[][].class).parametized(
				String.class);
		assertEquals("java.lang.Class<java.lang.String>",
				classArray.baseType().toString());
		assertEquals("java.lang.Class<java.lang.String>[][]",
				classArray.toString());
	}

	@Test
	public void testMultiDimensionalGenericArrays() {
		Type<Class[][][]> classArray = Type.raw(Class[][][].class).parametized(
				String.class);
		assertEquals("java.lang.Class<java.lang.String>",
				classArray.baseType().toString());
		assertEquals("java.lang.Class<java.lang.String>[][][]",
				classArray.toString());
	}

	@Test
	public void shouldRecognize1DimensionalArrayTypes() {
		Type<Number[]> t = Type.raw(Number[].class);
		assertEquals(1, t.arrayDimensions());
	}

	@Test
	public void shouldNotRecognizeMultiDimensionalArrayTypesAsArray1D() {
		Type<Number[][]> t = Type.raw(Number[][].class);
		assertEquals(2, t.arrayDimensions());
	}

	@Test
	public void testMorePreciseThan() {
		Type<Integer> integer = Type.raw(Integer.class);
		Type<Number> number = Type.raw(Number.class);
		assertMorePrecise(integer, number);
	}

	@Test
	public void testMorePreciseThanUpperBound() {
		Type<Integer> integer = Type.raw(Integer.class);
		Type<? extends Integer> upperBoundInteger = Type.raw(
				Integer.class).asUpperBound();
		assertMorePrecise(integer, upperBoundInteger);
	}

	@Test
	public void testMorePreciseThanIndependend() {
		Type<Integer> integer = Type.raw(Integer.class);
		Type<String> string = Type.raw(String.class);
		assertTrue(integer.moreQualifiedThan(string));
		assertTrue(string.moreQualifiedThan(integer));
	}

	private static void assertMorePrecise(Type<?> morePrecise,
			Type<?> lessPrecise) {
		assertTrue(morePrecise.moreQualifiedThan(lessPrecise));
		assertFalse(lessPrecise.moreQualifiedThan(morePrecise));
	}

	@Test
	public void testMorePreciseThanWithGenerics() {
		Type<List> integer = Type.raw(List.class).parametized(Integer.class);
		Type<List> number = Type.raw(List.class).parametized(Number.class);
		assertMorePrecise(integer, number);
	}

	@Test
	public void testMorePreciseThanWithGenericsAndWithout() {
		Type<List> integer = Type.raw(List.class).parametized(Integer.class);
		Type<List> wildcardList = Type.raw(List.class);
		assertMorePrecise(integer, wildcardList);
	}

	@Test
	public void testMorePreciseThanWithWildcardGenerics() {
		Type<List> integer = Type.raw(List.class).parametized(Integer.class);
		Type<List> wildcardInteger = Type.raw(List.class).parametized(
				Integer.class).parametizedAsUpperBounds();
		assertMorePrecise(integer, wildcardInteger);
	}

	@Test
	public void testMorePreciseThanWithSubclassAndGenerics() {
		Type<ArrayList> arrayListOfIntegers = Type.raw(
				ArrayList.class).parametized(Integer.class);
		Type<List> listOfIntegers = Type.raw(List.class).parametized(
				Integer.class);
		assertMorePrecise(arrayListOfIntegers, listOfIntegers);
	}

	@Test
	public void testArrayType() {
		Type<? extends Number> t = Type.raw(Number.class).asUpperBound();
		assertEquals("? extends java.lang.Number[]",
				t.addArrayDimension().toString());
	}

	@Test
	public void thatGenericSuperInterfacesAreAvailableAsSupertype() {
		Type<? super Integer>[] intergerSupertypes = Type.raw(
				Integer.class).supertypes();
		assertContains(intergerSupertypes,
				Type.raw(Comparable.class).parametized(Integer.class));
	}

	@Test
	public void thatTypeVariableSuperInterfacesAreAvailableAsSupertype() {
		Type<? super List>[] supertypes = Type.raw(List.class).parametized(
				String.class).supertypes();
		assertContains(supertypes,
				Type.raw(Collection.class).parametized(String.class));
	}

	@Test
	public void thatTypeVariableSuperSuperInterfacesAreAvailableAsSupertype() {
		Type<? super List>[] supertypes = Type.raw(List.class).parametized(
				String.class).supertypes();
		assertContains(supertypes,
				Type.raw(Iterable.class).parametized(String.class));
	}

	@Test
	public void thatTypeVariableSuperInterfacesUseWildcardDefaultAsSupertype() {
		Type<? super List>[] supertypes = Type.raw(List.class).supertypes();
		assertContains(supertypes,
				Type.raw(Collection.class).parametized(Type.WILDCARD));
	}

	@Test
	public void thatTypeVariableSuperClassIsAvailableAsSupertype() {
		Type<? super ArrayList>[] supertypes = Type.raw(
				ArrayList.class).parametized(String.class).supertypes();
		assertContains(supertypes,
				Type.raw(AbstractList.class).parametized(String.class));
	}

	@Test
	public void thatTypeVariableSuperSuperClassIsAvailableAsSupertype() {
		Type<? super ArrayList>[] supertypes = Type.raw(
				ArrayList.class).parametized(String.class).supertypes();
		assertContains(supertypes,
				Type.raw(AbstractCollection.class).parametized(String.class));
	}

	@Test
	public void thatTypeVariablesResolvedCorrectlyForSupertypes() {
		Type<Bar> type = Type.raw(Bar.class).parametized(Integer.class,
				Float.class);
		Type<? super Bar>[] supertypes = type.supertypes();
		assertContains(supertypes,
				Type.raw(Foo.class).parametized(Float.class, String.class));
		assertContains(supertypes,
				Type.raw(Baz.class).parametized(Integer.class));
		assertContains(supertypes,
				Type.raw(QuxQux.class).parametized(Float.class));
	}

	@Test
	public void thatCompareableSupertypeOfIntegerIsCompareableInteger() {
		assertTrue(Type.supertype(Comparable.class,
				Type.raw(Integer.class)).equalTo(
						Type.raw(Comparable.class).parametized(Integer.class)));
	}

	@Test
	public void thatTypeComparisonIsDoneBasedOnTheCommonType() {
		Type<List> stringList = Type.raw(List.class).parametized(String.class);
		assertTrue(Type.raw(XList.class).parametized(Integer.class,
				String.class).isAssignableTo(stringList));
		assertFalse(Type.raw(XList.class).parametized(String.class,
				Integer.class).isAssignableTo(stringList));
	}

	@Test(expected = IllegalArgumentException.class)
	public void thatUpperBoundOfParameterizedTypesIsChecked() {
		Type.raw(XList.class).parametized(Object.class, Object.class);
	}

	@Test
	public void thatRawTypeCanBeCastToAnySupertype() {
		Type<ArrayList> rawArrayList = raw(ArrayList.class);
		assertNotNull(rawArrayList.castTo(raw(List.class)));
		assertNotNull(rawArrayList.castTo(
				raw(List.class).parametized(Integer.class)));
		assertNotNull(rawArrayList.castTo(raw(List.class).parametized(
				Integer.class).parametizedAsUpperBounds()));
	}

	@Test
	public void thatUnparametizedTypeCanBeCastToAllItsSupertypes() {
		Type<Integer> integer = raw(Integer.class);
		for (Type<? super Integer> supertype : integer.supertypes()) {
			assertNotNull(integer.castTo(supertype));
		}
	}

	@Test(expected = ClassCastException.class)
	public void thatParameterizedTypeCannotBeCastToDifferentActualTypeParameterSupertype() {
		assertNotNull(raw(List.class).parametized(String.class).castTo(
				raw(Collection.class).parametized(Integer.class)));
	}

	@Test
	public void thatParameterizedTypeCanBeCastToWildcardedSupertype() {
		assertNotNull(raw(ArrayList.class).parametized(Integer.class).castTo(
				raw(List.class).parametized(
						Number.class).parametizedAsUpperBounds()));
	}

	@Test
	public void actualTypeArguments() throws Exception {
		assertEquals("{X=? extends java.io.Serializable, E=?}",
				classType(XList.class).actualTypeArguments().toString());
		assertEquals("{X=java.lang.String, E=java.lang.Integer}",
				raw(XList.class).parametized(String.class,
						Integer.class).actualTypeArguments().toString());
		assertEquals("{X=java.lang.String, E=?}",
				returnType(getClass().getMethod(
						"typeVariableWithActualTypeArgument")).actualTypeArguments().toString());
		assertEquals("{X=? extends java.lang.Number, E=java.lang.Integer}",
				returnType(getClass().getMethod(
						"typeVariableWithActualTypeArgument2")).actualTypeArguments().toString());
	}

	private static void assertContains(Type<?>[] actual, Type<?> expected) {
		for (Type<?> type : actual) {
			if (type.equalTo(expected)) {
				return;
			}
		}
		fail(Arrays.toString(actual) + " should have contained: " + expected);
	}
}
