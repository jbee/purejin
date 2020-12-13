package test.integration.api;

import org.junit.jupiter.api.Test;
import se.jbee.inject.lang.Type;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.lang.Type.*;

@SuppressWarnings({ "rawtypes" })
class TestType {

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
	void toStringPrintsFullyQualifiedGenericType() throws Exception {
		Type<List> l = raw(List.class).parametized(String.class);
		assertEquals("java.util.List<java.lang.String>", l.toString());

		l = raw(List.class).parametized(
				raw(String.class).asUpperBound());
		assertEquals("java.util.List<? extends java.lang.String>",
				l.toString());

		l = raw(List.class).parametized(
				raw(String.class)).parametizedAsUpperBounds();
		assertEquals("java.util.List<? extends java.lang.String>",
				l.toString());

		Field stringList = TestType.class.getDeclaredField("aStringListField");
		assertEquals("java.util.List<java.lang.String>",
				Type.fieldType(stringList).toString());
	}

	@Test
	void isAssignableToWithTypeAndItsSubtype() {
		Type<Integer> integer = raw(Integer.class);
		Type<Number> number = raw(Number.class);
		assertTrue(integer.isAssignableTo(number));
		assertFalse(number.isAssignableTo(integer));
	}

	@Test
	void isAssignableToWith1Generic() {
		Type<List> listOfIntegers = raw(List.class).parametized(
				Integer.class);
		Type<List> listOfNumbers = raw(List.class).parametized(
				Number.class);
		assertFalse(listOfIntegers.isAssignableTo(listOfNumbers));
		assertFalse(listOfNumbers.isAssignableTo(listOfIntegers));
		assertTrue(listOfIntegers.isAssignableTo(
				listOfNumbers.parametizedAsUpperBounds()));
		assertFalse(listOfNumbers.isAssignableTo(
				listOfIntegers.parametizedAsUpperBounds()));
	}

	@Test
	void isAssignableToWith2Generics() {
		Type<List> listOfClassesOfIntegers = raw(List.class).parametized(
				raw(Class.class).parametized(Integer.class));
		Type<Class> classOfNumbers = raw(Class.class).parametized(
				Number.class);
		Type<List> listOfClassesOfNumbers = raw(List.class).parametized(
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
	void genericArrays() {
		Type<Class[]> classArray = raw(Class[].class).parametized(
				String.class);
		assertEquals("java.lang.Class<java.lang.String>",
				classArray.baseType().toString());
		assertEquals("java.lang.Class<java.lang.String>[]",
				classArray.toString());
	}

	@Test
	void twoDimensionalGenericArrays() {
		Type<Class[][]> classArray = raw(Class[][].class).parametized(
				String.class);
		assertEquals("java.lang.Class<java.lang.String>",
				classArray.baseType().toString());
		assertEquals("java.lang.Class<java.lang.String>[][]",
				classArray.toString());
	}

	@Test
	void multiDimensionalGenericArrays() {
		Type<Class[][][]> classArray = raw(Class[][][].class).parametized(
				String.class);
		assertEquals("java.lang.Class<java.lang.String>",
				classArray.baseType().toString());
		assertEquals("java.lang.Class<java.lang.String>[][][]",
				classArray.toString());
	}

	@Test
	void shouldRecognize1DimensionalArrayTypes() {
		assertEquals(1, raw(Number[].class).arrayDimensions());
	}

	@Test
	void shouldNotRecognizeMultiDimensionalArrayTypesAsArray1D() {
		assertEquals(2, raw(Number[][].class).arrayDimensions());
	}

	@Test
	void subtypesAreMoreQualifiedThanTheirSupertypes() {
		assertMoreQualified(raw(Integer.class), raw(Number.class));
	}

	@Test
	void definiteTypesAreMoreQualifiedThanTheirThanUpperBound() {
		assertMoreQualified(raw(Integer.class),
				raw(Integer.class).asUpperBound());
	}

	@Test
	void typesGenerallyAreMoreQualifiedThanAnyNonAssignableType() {
		Type<Integer> integer = raw(Integer.class);
		Type<String> string = raw(String.class);
		assertTrue(integer.moreQualifiedThan(string));
		assertTrue(string.moreQualifiedThan(integer));
	}

	private static void assertMoreQualified(Type<?> more, Type<?> less) {
		assertTrue(more.moreQualifiedThan(less));
		assertFalse(less.moreQualifiedThan(more));
	}

	@Test
	void subtypesAreMoreQualifiedThanTheirSupertypesWhenUsedAsGeneric() {
		assertMoreQualified(raw(List.class).parametized(Integer.class),
				raw(List.class).parametized(Number.class));
	}

	@Test
	void definiteGenericTypesAreMoreQualifiedThanTheirRawTypes() {
		assertMoreQualified(raw(List.class).parametized(Integer.class),
				raw(List.class));
	}

	@Test
	void definiteGenericTypesAreMoreQualifiedThanTheirWildcardTypes() {
		assertMoreQualified(raw(List.class).parametized(Integer.class),
				raw(List.class).parametized(
						Integer.class).parametizedAsUpperBounds());
	}

	@Test
	void subtypesWithGenericIsMoreQualifiedThanItsSubpertypeWithSameGeneric() {
		assertMoreQualified(raw(ArrayList.class).parametized(Integer.class),
				raw(List.class).parametized(Integer.class));
	}

	@Test
	void addArrayDimension() {
		Type<? extends Number> t = raw(Number.class).asUpperBound();
		assertEquals("? extends java.lang.Number[]",
				t.addArrayDimension().toString());
	}

	@Test
	void genericSuperInterfacesAreContainedInSupertypes() {
		assertContains(raw(Integer.class).supertypes(),
				raw(Comparable.class).parametized(Integer.class));
	}

	@Test
	void typeVariableSuperInterfacesAreContainedInSupertypes() {
		assertContains(raw(List.class).parametized(String.class).supertypes(),
				raw(Collection.class).parametized(String.class));
	}

	@Test
	void typeVariableSuperSuperInterfacesAreContainedInSupertypes() {
		assertContains(raw(List.class).parametized(String.class).supertypes(),
				raw(Iterable.class).parametized(String.class));
	}

	@Test
	void typeVariableSuperInterfacesOfRawTypesUseWildcardsInSupertypes() {
		assertContains(raw(List.class).supertypes(),
				raw(Collection.class).parametized(Type.WILDCARD));
	}

	@Test
	void typeVariableSuperClassIsContainedInSupertypes() {
		assertContains(
				raw(ArrayList.class).parametized(String.class).supertypes(),
				raw(AbstractList.class).parametized(String.class));
	}

	@Test
	void typeVariableSuperSuperClassIsContainedInSupertypes() {
		assertContains(
				raw(ArrayList.class).parametized(String.class).supertypes(),
				raw(AbstractCollection.class).parametized(String.class));
	}

	@Test
	void typeVariablesAreContainedResolvedInSupertypes() {
		Type<? super Bar>[] supertypes = raw(Bar.class).parametized(
				Integer.class, Float.class).supertypes();
		assertContains(supertypes,
				raw(Foo.class).parametized(Float.class, String.class));
		assertContains(supertypes,
				raw(Baz.class).parametized(Integer.class));
		assertContains(supertypes,
				raw(QuxQux.class).parametized(Float.class));
	}

	@Test
	void comparableSupertypeOfIntegerIsComparableInteger() {
		assertTrue(Type.supertype(Comparable.class,
				raw(Integer.class)).equalTo(
						raw(Comparable.class).parametized(Integer.class)));
	}

	@Test
	void typeComparisonIsDoneBasedOnTheCommonType() {
		Type<List> stringList = raw(List.class).parametized(String.class);
		assertTrue(raw(XList.class).parametized(Integer.class,
				String.class).isAssignableTo(stringList));
		assertFalse(raw(XList.class).parametized(String.class,
				Integer.class).isAssignableTo(stringList));
	}

	@Test
	void upperBoundOfParameterizedTypesIsChecked() {
		Type<XList> listType = raw(XList.class);
		assertThrows(IllegalArgumentException.class,
				() -> listType.parametized(Object.class, Object.class));
	}

	@Test
	void rawTypeCanBeCastToAnySupertype() {
		Type<ArrayList> rawArrayList = raw(ArrayList.class);
		assertNotNull(rawArrayList.castTo(raw(List.class)));
		assertNotNull(rawArrayList.castTo(
				raw(List.class).parametized(Integer.class)));
		assertNotNull(rawArrayList.castTo(raw(List.class).parametized(
				Integer.class).parametizedAsUpperBounds()));
	}

	@Test
	void simpleTypeCanBeCastToAllItsSupertypes() {
		Type<Integer> integer = raw(Integer.class);
		for (Type<? super Integer> supertype : integer.supertypes()) {
			assertNotNull(integer.castTo(supertype));
		}
	}

	@Test
	void parameterizedTypeCannotBeCastToDifferentActualTypeParameterSupertypes() {
		Type<List> a = raw(List.class).parametized(String.class);
		Type<Collection> b = raw(Collection.class).parametized(Integer.class);
		assertThrows(ClassCastException.class, () -> a.castTo(b));
	}

	@Test
	void parameterizedTypeCanBeCastToWildcardSupertypes() {
		assertNotNull(raw(ArrayList.class).parametized(Integer.class).castTo(
				raw(List.class).parametized(
						Number.class).parametizedAsUpperBounds()));
	}

	@Test
	void actualTypeArguments() throws Exception {
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

	interface SimpleMap<A,B> {

		B get(A key);

		void put(A key, B value);

		Set<A> keys();
	}

	abstract static class SimpleMapImpl<A,B> implements SimpleMap<A, B> {

		Map<A, B> values;
	}

	@Test
	void actualTypeOfMethodReturnTypeWithTypeLevelTypeParameter()
			throws Exception {
		Type<SimpleMap> actualMapType = raw(SimpleMap.class) //
				.parametized(String.class, Integer.class);

		Method get = SimpleMap.class.getMethod("get", Object.class);
		assertEquals(raw(Integer.class),
				Type.actualReturnType(get, actualMapType));

		Method keys = SimpleMap.class.getMethod("keys");
		assertEquals(raw(Set.class).parametized(String.class),
				Type.actualReturnType(keys, actualMapType));
	}

	@Test
	void actualTypeOfParameterTypeWithTypeLevelTypeParameter()
			throws Exception {
		Type<SimpleMap> actualMapType = raw(SimpleMap.class) //
				.parametized(String.class, Integer.class);

		Method put = SimpleMap.class.getMethod("put", Object.class, Object.class);
		assertEquals(raw(String.class),
				Type.actualParameterType(put.getParameters()[0], actualMapType));
		assertEquals(raw(Integer.class),
				Type.actualParameterType(put.getParameters()[1], actualMapType));
	}

	@Test
	void actualTypeOfFieldTypeWithTypeLevelTypeParameter() throws Exception {
		Type<SimpleMapImpl> actualMapType = raw(SimpleMapImpl.class) //
				.parametized(String.class, Integer.class);

		Field values = SimpleMapImpl.class.getDeclaredField("values");
		assertEquals(raw(Map.class).parametized(String.class, Integer.class),
				Type.actualFieldType(values, actualMapType));
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
