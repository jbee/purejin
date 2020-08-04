/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static java.lang.System.arraycopy;
import static java.lang.reflect.Array.newInstance;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isPrivate;
import static java.lang.reflect.Modifier.isProtected;
import static java.lang.reflect.Modifier.isPublic;
import static java.util.Arrays.copyOf;
import static se.jbee.inject.Type.raw;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import se.jbee.inject.UnresolvableDependency.NoMethodForDependency;
import se.jbee.inject.UnresolvableDependency.SupplyFailed;

/**
 * Language level utility methods for the library.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
@SuppressWarnings({ "squid:S1200", "squid:S1448" })
public final class Utils {

	/**
	 * Function that determines of two elements of the same type are equal.
	 *
	 * @param <T> type of the elements compared
	 */
	@FunctionalInterface
	public interface Eq<T> extends BiPredicate<T, T> {
	}

	/* Arrays */

	public static <A> A[] arrayFilter(A[] arr, Predicate<A> accept) {
		if (arr == null || arr.length == 0)
			return arr;
		A[] accepted = newArray(arr, arr.length);
		int j = 0;
		for (A a : arr)
			if (accept.test(a))
				accepted[j++] = a;
		return j == arr.length ? arr : copyOf(accepted, j);
	}

	public static <A> A[] arrayAppend(A[] arr, A e) {
		A[] copy = copyOf(arr, arr.length + 1);
		copy[arr.length] = e;
		return copy;
	}

	public static <A> A[] arrayPrepand(A e, A[] arr) {
		A[] copy = newArray(arr, arr.length + 1);
		arraycopy(arr, 0, copy, 1, arr.length);
		copy[0] = e;
		return copy;
	}

	public static <A> A[] arrayDropTail(A[] arr, int n) {
		if (arr.length <= n)
			return newArray(arr, 0);
		return copyOf(arr, arr.length - n);
	}

	/**
	 * This is a special form of flatmap where the mapping does not return zero
	 * to n Bs but always returns a B. If {@code null} is returned this means
	 * "zero" or remove the B. So length of {@code B[]} is always {@code <=}
	 * length of {@code A[]}.
	 *
	 * @param <A> source element type
	 * @param <B> target element type
	 * @param from source array
	 * @param to target element {@link Class} (needed to create correct target
	 *            array)
	 * @param flatmapOp function applied to each source element to compute the
	 *            target element, returns {@code null} for elements that should
	 *            be filtered (not be included in target array)
	 * @return target array with a similar or shorter length as the source
	 *         array, never null
	 */
	public static <A, B> B[] arrayFlatmap(A[] from, Class<B> to,
			Function<A, B> flatmapOp) {
		B[] bs = newArray(to, from.length);
		int j = 0;
		for (A a : from) {
			B b = flatmapOp.apply(a);
			if (b != null)
				bs[j++] = b;
		}
		return j == from.length ? bs : copyOf(bs, j);
	}

	public static <A, B> B[] arrayMap(A[] as, Class<B> to,
			Function<A, B> mapOp) {
		B[] bs = newArray(to, as.length);
		for (int i = 0; i < as.length; i++)
			bs[i] = mapOp.apply(as[i]);
		return bs;
	}

	public static <A> A[] arrayMap(A[] arr, UnaryOperator<A> mapOp) {
		if (arr == null || arr.length == 0)
			return arr;
		A[] mapped = newArray(arr, arr.length);
		for (int i = 0; i < arr.length; i++)
			mapped[i] = mapOp.apply(arr[i]);
		return mapped;
	}

	public static <A> int arrayIndex(A[] arr, A e, Eq<A> eq) {
		if (arr == null || arr.length == 0)
			return -1;
		for (int i = 0; i < arr.length; i++)
			if (eq.test(e, arr[i]))
				return i;
		return -1;
	}

	public static <A> A arrayFindFirst(A[] arr, Predicate<A> test) {
		if (arr == null || arr.length == 0)
			return null;
		for (A a : arr)
			if (test.test(a))
				return a;
		return null;
	}

	public static <A> boolean arrayContains(A[] arr, Predicate<A> test) {
		return arrayFindFirst(arr, test) != null;
	}

	public static <A> boolean arrayContains(A[] arr, A e, Eq<A> eq) {
		if (arr == null || arr.length == 0)
			return false;
		for (A a : arr)
			if (eq.test(a, e))
				return true;
		return false;
	}

	public static <A> boolean arrayEquals(A[] a, A[] b, Eq<A> eq) {
		if (a == b)
			return true;
		if (b == null || a == null || a.length != b.length)
			return false;
		for (int i = 0; i < a.length; i++)
			if (!eq.test(a[i], b[i]))
				return false;
		return true;
	}

	public static <A extends Comparable<A>> int arrayCompare(A[] a, A[] b) {
		if (a == b)
			return 0;
		int res = Integer.compare(a.length, b.length);
		if (res != 0)
			return res;
		for (int i = 0; i < a.length; i++) {
			res = a[i].compareTo(b[i]);
			if (res != 0)
				return res;
		}
		return 0;
	}

	@SuppressWarnings("unchecked")
	public static <A> A[] newArray(A[] proto, int length) {
		return (A[]) newArray(proto.getClass().getComponentType(), length);
	}

	@SuppressWarnings("unchecked")
	public static <A> A[] newArray(Class<A> componentType, int length) {
		return (A[]) newInstance(componentType, length);
	}

	public static <A> A[] arrayOf(Collection<? extends A> list, Class<A> type) {
		return list.toArray(newArray(type, list.size()));
	}

	/* Annotations */

	public static Method annotationPropertyByType(Class<?> type,
			Class<? extends Annotation> annotation) {
		return arrayFindFirst(annotation.getDeclaredMethods(),
				m -> m.getReturnType() == type);
	}

	public static Name annotatedName(Method nameProperty, Annotation obj) {
		if (obj == null)
			return null;
		try {
			String name = (String) nameProperty.invoke(obj);
			if (!name.isEmpty() && !name.equals(nameProperty.getDefaultValue()))
				return Name.named(name);
		} catch (Exception e) {
			// fall through
		}
		return null;
	}

	@SuppressWarnings("LoopConditionNotUpdatedInsideLoop")
	public static <A extends Annotation> A annotation(Class<A> type,
			AnnotatedElement obj) {
		A res = obj.getAnnotation(type);
		if (res != null)
			return res;
		if (!isAllowedOnAnnotations(type))
			return null;
		do {
			for (Annotation a : obj.getAnnotations()) {
				res = annotation(type, a.annotationType());
				if (res != null)
					return res;
			}
		} while (obj instanceof Class && obj != Object.class);
		return null;
	}

	public static boolean isAllowedOnAnnotations(
			Class<? extends Annotation> type) {
		return type.isAnnotationPresent(Target.class)
			&& arrayContains(type.getAnnotation(Target.class).value(),
					e -> e == ElementType.ANNOTATION_TYPE);
	}

	/* Classes / Types */

	/**
	 * @param <T> actual object type
	 * @param obj the {@link AccessibleObject} to make accessible
	 * @return the given object made accessible.
	 */
	public static <T extends AccessibleObject> T accessible(T obj) {
		obj.setAccessible(true);
		return obj;
	}

	/**
	 * @param cls Any {@link Class} object, not null
	 * @return A {@link Class} counts as "virtual" when it is known that its not
	 *         a type handled by an {@link Injector} context, either because it
	 *         cannot be constructed at all or it does not make sense to let the
	 *         {@link Injector} take care of it. This includes value types,
	 *         enums, collection types (including arrays) or any type than
	 *         cannot be instantiated by its nature (abstract types).
	 *
	 *         Note that this method just covers JRE types.
	 */
	@SuppressWarnings({ "squid:S1067", "squid:S1541" })
	public static boolean isClassVirtual(Class<?> cls) {
		return cls == null || cls.isInterface() || cls.isEnum()
			|| cls.isAnnotation() || cls.isAnonymousClass() || cls.isPrimitive()
			|| cls.isArray() || isAbstract(cls.getModifiers())
			|| cls == String.class || Number.class.isAssignableFrom(cls)
			|| cls == Boolean.class || cls == Void.class || cls == Class.class;
	}

	public static boolean isClassInstantiable(Class<?> cls) {
		return !isClassVirtual(cls) && cls.getTypeParameters().length == 0;
	}

	/**
	 * @param cls Any {@link Class} object, not null
	 * @return A {@link Class} is monomodal if it there is just a single
	 *         possible initial state. All newly created instances can just have
	 *         this similar initial state but due to internal state they could
	 *         (not necessarily must) develop (behave) different later on.
	 *
	 *         The opposite of monomodal is multimodal.
	 */
	public static boolean isClassMonomodal(Class<?> cls) {
		if (cls.isInterface())
			return false;
		if (cls == Object.class)
			return true;
		for (Field f : cls.getDeclaredFields())
			if (!Modifier.isStatic(f.getModifiers()))
				return false;
		for (Constructor<?> c : cls.getDeclaredConstructors())
			// maybe arguments are passed to super-type so we check it too
			if (c.getParameterCount() > 0)
				return isClassMonomodal(cls.getSuperclass());
		return true;
	}

	/**
	 * @param cls Any {@link Class} object, not null
	 * @return true, if the argument is a {@link Class} that is a simple
	 *         standard <code>class</code> with only a default constructor.
	 */
	public static boolean isClassBanal(Class<?> cls) {
		return !cls.isInterface() && !isAbstract(cls.getModifiers())
			&& !cls.isEnum() && !cls.isAnnotation() && !cls.isArray()
			&& cls.getDeclaredConstructors().length == 1
			&& cls.getDeclaredConstructors()[0].getParameterCount() == 0
			&& isClassMonomodal(cls);
	}

	/* Members */

	public static <T extends Member> T moreVisible(T a, T b) {
		int am = a.getModifiers();
		if (isPublic(am))
			return a;
		int bm = b.getModifiers();
		if (isPublic(bm))
			return b;
		if (isProtected(am))
			return a;
		if (isProtected(bm))
			return b;
		if (isPrivate(bm))
			return a;
		if (isPrivate(am))
			return b;
		return a; // same
	}

	/**
	 * Returns the constructor with most visible visibility and longest argument
	 * list. Self-referencing constructors are ignored.
	 *
	 * @param <T> type that should be constructed/instantiated
	 * @param type constructed type
	 * @return The highest visibility constructor with the most parameters that
	 *         does not have the declaring class itself as parameter type (some
	 *         compiler seam to generate such a synthetic constructor)
	 * @throws NoMethodForDependency in case the type is not constructable (has
	 *             no constructors at all)
	 */
	public static <T> Constructor<T> commonConstructor(Class<T> type)
			throws NoMethodForDependency {
		@SuppressWarnings("unchecked")
		Constructor<T>[] cs = (Constructor<T>[]) type.getDeclaredConstructors();
		if (cs.length == 0)
			throw new NoMethodForDependency(raw(type));
		if (cs.length == 1)
			return cs[0];
		Constructor<T> mostParamsConstructor = null;
		for (Constructor<T> c : cs)
			mostParamsConstructor = commonConstructor(type,
					mostParamsConstructor, c);
		if (mostParamsConstructor == null)
			throw new NoMethodForDependency(raw(type));
		return mostParamsConstructor;
	}

	private static <T> Constructor<T> commonConstructor(Class<T> type,
			Constructor<T> a, Constructor<T> b) {
		return !arrayContains(b.getParameterTypes(), type, Class::equals) // avoid self referencing constructors (synthetic) as they cause endless loop
			&& (a == null //
				|| (moreVisible(b, a) == b && (moreVisible(a, b) == b
					|| b.getParameterCount() > a.getParameterCount()))) ? b : a;
	}

	public static <T> Constructor<T> commonConstructorOrNull(Class<T> type) {
		try {
			return commonConstructor(type);
		} catch (RuntimeException e) {
			return null;
		}
	}

	public static <T> Constructor<T> noArgsConstructor(Class<T> type) {
		if (type.isInterface() || type.isEnum() || type.isPrimitive())
			throw new NoMethodForDependency(raw(type));
		try {
			return type.getDeclaredConstructor();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new NoMethodForDependency(raw(type), new Type[0], e);
		}
	}

	/* Sequences */

	public static boolean seqRegionEquals(CharSequence s1, CharSequence s2,
			int length) {
		if (s1.length() < length || s2.length() < length)
			return false;
		if (s1 == s2)
			return true;
		for (int i = length - 1; i > 0; i--)
			if (s1.charAt(i) != s2.charAt(i))
				return false;
		return true;
	}

	public static int seqCount(CharSequence seq, char match) {
		int c = 0;
		for (int i = seq.length() - 1; i > 0; i--)
			if (seq.charAt(i) == match)
				c++;
		return c;
	}

	/* Exception Handling */

	public static <T> T orElse(T defaultValue, Provider<T> src) {
		try {
			return src.provide();
		} catch (UnresolvableDependency e) {
			return defaultValue;
		}
	}

	/* Object Instantiation and Method Invocation */

	public static <T> T construct(Constructor<T> target, Object... args)
			throws SupplyFailed {
		try {
			return target.newInstance(args);
		} catch (Exception e) {
			throw SupplyFailed.valueOf(e, target);
		}
	}

	public static Object produce(Method target, Object owner, Object... args)
			throws SupplyFailed {
		try {
			return target.invoke(owner, args);
		} catch (Exception e) {
			throw SupplyFailed.valueOf(e, target);
		}
	}

	public static Object share(Field target, Object owner) throws SupplyFailed {
		try {
			return target.get(owner);
		} catch (Exception e) {
			throw SupplyFailed.valueOf(e, target);
		}
	}

	public static <T> T instantiate(Class<T> type) {
		return construct(accessible(noArgsConstructor(type)));
	}
}