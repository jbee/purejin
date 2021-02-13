package test.integration.api;

import org.junit.jupiter.api.Test;
import se.jbee.lang.Type;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.lang.Cast.listTypeOf;
import static se.jbee.lang.Type.*;
import static se.jbee.junit.assertion.Assertions.assertToStringEquals;

@SuppressWarnings({ "rawtypes" })
class TestType {

	private interface Baz<T> {
		// needed to check supertypes() method
	}

	private static class Bar<A, B> extends Foo<B, String> implements Baz<A> {
		// needed to check supertypes() method
	}

	private static class Foo<A, B> extends Qux<A> implements RandomAccess {
		// needed to check supertypes() method
	}

	private static class Qux<R> implements QuxQux<R> {
		// needed to check supertypes() method
	}

	private interface QuxQux<T> extends Serializable {
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
		Type<List> l = raw(List.class).parameterized(String.class);
		assertEquals("java.util.List<java.lang.String>", l.toString());

		l = raw(List.class).parameterized(
				raw(String.class).asUpperBound());
		assertEquals("java.util.List<? extends java.lang.String>",
				l.toString());

		l = raw(List.class).parameterized(
				raw(String.class)).parameterizedAsUpperBounds();
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
		Type<List> listOfIntegers = raw(List.class).parameterized(
				Integer.class);
		Type<List> listOfNumbers = raw(List.class).parameterized(
				Number.class);
		assertFalse(listOfIntegers.isAssignableTo(listOfNumbers));
		assertFalse(listOfNumbers.isAssignableTo(listOfIntegers));
		assertTrue(listOfIntegers.isAssignableTo(
				listOfNumbers.parameterizedAsUpperBounds()));
		assertFalse(listOfNumbers.isAssignableTo(
				listOfIntegers.parameterizedAsUpperBounds()));
	}

	@Test
	void isAssignableToWith2Generics() {
		Type<List> listOfClassesOfIntegers = raw(List.class).parameterized(
				raw(Class.class).parameterized(Integer.class));
		Type<Class> classOfNumbers = raw(Class.class).parameterized(
				Number.class);
		Type<List> listOfClassesOfNumbers = raw(List.class).parameterized(
				classOfNumbers);
		assertFalse(
				listOfClassesOfIntegers.isAssignableTo(listOfClassesOfNumbers));
		assertFalse(listOfClassesOfIntegers.isAssignableTo(
				listOfClassesOfNumbers.parameterizedAsUpperBounds()));
		Type<List> listOfExtendsClassesOfExtendsNumbers = listOfClassesOfNumbers.parameterized(
				classOfNumbers.asUpperBound().parameterizedAsUpperBounds());
		assertTrue(listOfClassesOfIntegers.isAssignableTo(
				listOfExtendsClassesOfExtendsNumbers));
	}

	@Test
	void genericArrays() {
		Type<Class[]> classArray = raw(Class[].class).parameterized(
				String.class);
		assertEquals("java.lang.Class<java.lang.String>",
				classArray.baseType().toString());
		assertEquals("java.lang.Class<java.lang.String>[]",
				classArray.toString());
	}

	@Test
	void twoDimensionalGenericArrays() {
		Type<Class[][]> classArray = raw(Class[][].class).parameterized(
				String.class);
		assertEquals("java.lang.Class<java.lang.String>",
				classArray.baseType().toString());
		assertEquals("java.lang.Class<java.lang.String>[][]",
				classArray.toString());
	}

	@Test
	void multiDimensionalGenericArrays() {
		Type<Class[][][]> classArray = raw(Class[][][].class).parameterized(
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
		assertMoreQualified(raw(List.class).parameterized(Integer.class),
				raw(List.class).parameterized(Number.class));
	}

	@Test
	void definiteGenericTypesAreMoreQualifiedThanTheirRawTypes() {
		assertMoreQualified(raw(List.class).parameterized(Integer.class),
				raw(List.class));
	}

	@Test
	void definiteGenericTypesAreMoreQualifiedThanTheirWildcardTypes() {
		assertMoreQualified(raw(List.class).parameterized(Integer.class),
				raw(List.class).parameterized(
						Integer.class).parameterizedAsUpperBounds());
	}

	@Test
	void subtypesWithGenericIsMoreQualifiedThanItsSubpertypeWithSameGeneric() {
		assertMoreQualified(raw(ArrayList.class).parameterized(Integer.class),
				raw(List.class).parameterized(Integer.class));
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
				raw(Comparable.class).parameterized(Integer.class));
	}

	@Test
	void typeVariableSuperInterfacesAreContainedInSupertypes() {
		assertContains(raw(List.class).parameterized(String.class).supertypes(),
				raw(Collection.class).parameterized(String.class));
	}

	@Test
	void typeVariableSuperSuperInterfacesAreContainedInSupertypes() {
		assertContains(raw(List.class).parameterized(String.class).supertypes(),
				raw(Iterable.class).parameterized(String.class));
	}

	@Test
	void typeVariableSuperInterfacesOfRawTypesUseWildcardsInSupertypes() {
		assertContains(raw(List.class).supertypes(),
				raw(Collection.class).parameterized(Type.WILDCARD));
	}

	@Test
	void typeVariableSuperClassIsContainedInSupertypes() {
		assertContains(
				raw(ArrayList.class).parameterized(String.class).supertypes(),
				raw(AbstractList.class).parameterized(String.class));
	}

	@Test
	void typeVariableSuperSuperClassIsContainedInSupertypes() {
		assertContains(
				raw(ArrayList.class).parameterized(String.class).supertypes(),
				raw(AbstractCollection.class).parameterized(String.class));
	}

	@Test
	void typeVariablesAreContainedResolvedInSupertypes() {
		Set<Type<? super Bar>> supertypes = raw(Bar.class).parameterized(
				Integer.class, Float.class).supertypes();
		assertContains(supertypes,
				raw(Foo.class).parameterized(Float.class, String.class));
		assertContains(supertypes,
				raw(Baz.class).parameterized(Integer.class));
		assertContains(supertypes,
				raw(QuxQux.class).parameterized(Float.class));
		assertEquals(
				"[test.integration.api.TestType.Baz<java.lang.Integer>, " +
						"test.integration.api.TestType.Foo<java.lang.Float,java.lang.String>, " +
						"java.util.RandomAccess, test.integration.api.TestType.Qux<java.lang.Float>, " +
						"test.integration.api.TestType.QuxQux<java.lang.Float>, " +
						"java.io.Serializable, java.lang.Object]",
				supertypes.toString());
	}

	@Test
	void comparableSupertypeOfIntegerIsComparableInteger() {
		assertTrue(raw(Integer.class).toSuperType(Comparable.class).equalTo(
				raw(Comparable.class).parameterized(Integer.class)));
	}

	@Test
	void supertypesContainsAllSuperClasses() {
		Set<Type<? super Integer>> supertypes = raw(Integer.class).supertypes();
		assertContains(supertypes, raw(Number.class));
		assertContains(supertypes, OBJECT);
	}

	@Test
	void supertypesContainsAllSuperInterfaces() {
		Type<Integer> integer = raw(Integer.class);
		Set<Type<? super Integer>> supertypes = integer.supertypes();
		assertContains(supertypes, raw(Comparable.class).parameterized(Integer.class));
		assertContains(supertypes, raw(Serializable.class));
		// later java versions have more, check all of Integer
		for (Class<?> i : Integer.class.getInterfaces())
			assertContains(supertypes, integer.toSuperType(i));
		for (Class<?> i : Number.class.getInterfaces())
			assertContains(supertypes, integer.toSuperType(i));
		for (Class<?> i : Object.class.getInterfaces())
			assertContains(supertypes, integer.toSuperType(i));
	}

	@Test
	void typeComparisonIsDoneBasedOnTheCommonType() {
		Type<List> stringList = raw(List.class).parameterized(String.class);
		assertTrue(raw(XList.class).parameterized(Integer.class,
				String.class).isAssignableTo(stringList));
		assertFalse(raw(XList.class).parameterized(String.class,
				Integer.class).isAssignableTo(stringList));
	}

	@Test
	void upperBoundOfParameterizedTypesIsChecked() {
		Type<XList> listType = raw(XList.class);
		assertThrows(IllegalArgumentException.class,
				() -> listType.parameterized(Object.class, Object.class));
	}

	@Test
	void rawTypeCanBeCastToAnySupertype() {
		Type<ArrayList> rawArrayList = raw(ArrayList.class);
		assertNotNull(rawArrayList.castTo(raw(List.class)));
		assertNotNull(rawArrayList.castTo(
				raw(List.class).parameterized(Integer.class)));
		assertNotNull(rawArrayList.castTo(raw(List.class).parameterized(
				Integer.class).parameterizedAsUpperBounds()));
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
		Type<List> a = raw(List.class).parameterized(String.class);
		Type<Collection> b = raw(Collection.class).parameterized(Integer.class);
		assertThrows(ClassCastException.class, () -> a.castTo(b));
	}

	@Test
	void parameterizedTypeCanBeCastToWildcardSupertypes() {
		assertNotNull(raw(ArrayList.class).parameterized(Integer.class).castTo(
				raw(List.class).parameterized(
						Number.class).parameterizedAsUpperBounds()));
	}

	@Test
	void actualTypeArguments() throws Exception {
		assertToStringEquals("{X=? extends java.io.Serializable, E=?}",
				classType(XList.class).actualTypeArguments());
		assertToStringEquals("{X=java.lang.String, E=java.lang.Integer}",
				raw(XList.class).parameterized(String.class,
						Integer.class).actualTypeArguments());
		assertToStringEquals("{X=java.lang.String, E=?}",
				returnType(getClass().getMethod(
						"typeVariableWithActualTypeArgument")).actualTypeArguments());
		assertToStringEquals("{X=? extends java.lang.Number, E=java.lang.Integer}",
				returnType(getClass().getMethod(
						"typeVariableWithActualTypeArgument2")).actualTypeArguments());
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
				.parameterized(String.class, Integer.class);

		Method get = SimpleMap.class.getMethod("get", Object.class);
		assertEquals(raw(Integer.class),
				Type.actualReturnType(get, actualMapType));

		Method keys = SimpleMap.class.getMethod("keys");
		assertEquals(raw(Set.class).parameterized(String.class),
				Type.actualReturnType(keys, actualMapType));
	}

	@Test
	void actualTypeOfParameterTypeWithTypeLevelTypeParameter()
			throws Exception {
		Type<SimpleMap> actualMapType = raw(SimpleMap.class) //
				.parameterized(String.class, Integer.class);

		Method put = SimpleMap.class.getMethod("put", Object.class, Object.class);
		assertEquals(raw(String.class),
				Type.actualParameterType(put.getParameters()[0], actualMapType));
		assertEquals(raw(Integer.class),
				Type.actualParameterType(put.getParameters()[1], actualMapType));
	}

	@Test
	void actualTypeOfFieldTypeWithTypeLevelTypeParameter() throws Exception {
		Type<SimpleMapImpl> actualMapType = raw(SimpleMapImpl.class) //
				.parameterized(String.class, Integer.class);

		Field values = SimpleMapImpl.class.getDeclaredField("values");
		assertEquals(raw(Map.class).parameterized(String.class, Integer.class),
				Type.actualFieldType(values, actualMapType));
	}

	@Test
	void actualInstanceTypeWithoutGenericsIsRawInstanceType() {
		assertEquals(raw(String.class),
				actualInstanceType("Hello", raw(String.class)));
	}

	@Test
	void actualInstanceTypeWithoutGenericsAsInterfaceIsRawInstanceType() {
		assertEquals(raw(String.class),
				actualInstanceType("Hello", raw(CharSequence.class)));
	}

	@Test
	void actualInstanceTypeWithoutGenericsAsSupertypeIsRawInstanceType() {
		assertEquals(raw(String.class),
				actualInstanceType("Hello", raw(Object.class)));
	}

	@Test
	void actualInstanceTypeWithGenericsIsNotYetSupported() {
		ArrayList<String> obj = new ArrayList<>();
		Type<List<String>> actualType = listTypeOf(String.class);
		assertThrows(UnsupportedOperationException.class,
				() -> actualInstanceType(obj, actualType));
	}

	@Test
	void recursiveSuperTypesDoNotCauseStackOverflow() {
		Type<ElementType> enumType = classType(ElementType.class);
		assertNotNull(enumType);
		assertEquals("java.lang.Enum<java.lang.annotation.ElementType>",
				enumType.toSuperType(Enum.class).toString());
	}

	interface RecursiveType<T extends RecursiveType<T>> {

	}

	@Test
	void recursiveTypesDoNotCauseStackOverflow() {
		assertEquals(
				"test.integration.api.TestType.RecursiveType<? extends test.integration.api.TestType.RecursiveType<?>>",
				Type.classType(RecursiveType.class).toString());
	}

	private static void assertContains(Set<? extends Type<?>> actual, Type<?> expected) {
		for (Type<?> type : actual) {
			if (type.equalTo(expected)) {
				return;
			}
		}
		fail(actual + " should have contained: " + expected);
	}
}
