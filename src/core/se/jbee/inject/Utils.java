/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static java.lang.System.arraycopy;
import static java.lang.reflect.Array.newInstance;
import static java.util.Arrays.copyOf;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Language level utility methods for the library.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Utils {

	/**
	 * Function that determines of two elements of the same type are equal.
	 * 
	 * @param <T> type of the elements compared
	 */
	public interface Eq<T> extends BiPredicate<T, T> {
	}

	/* Arrays */

	public static <A> A[] arrayFilter(A[] arr, Predicate<A> accept) {
		if (arr == null || arr.length == 0)
			return arr;
		A[] accepted = newArray(arr, arr.length);
		int j = 0;
		for (int i = 0; i < arr.length; i++)
			if (accept.test(arr[i]))
				accepted[j++] = arr[i];
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

	public static <A> A[] arrayInsert(A e, A[] arr, Eq<A> eq) {
		if (arr.length == 0)
			return arrayPrepand(e, arr);
		int i = arrayIndex(arr, e, eq);
		if (i >= 0) {
			if (e == arr[i]) // already very same
				return arr;
			A[] tmp = arr.clone();
			tmp[i] = e;
			return tmp;
		}
		return arrayPrepand(e, arr);
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
	 */
	public static <A, B> B[] arrayFlatmap(A[] as, Class<B> to,
			Function<A, B> flatmapOp) {
		B[] bs = newArray(to, as.length);
		int j = 0;
		for (int i = 0; i < as.length; i++) {
			B b = flatmapOp.apply(as[i]);
			if (b != null)
				bs[j++] = b;
		}
		return j == as.length ? bs : copyOf(bs, j);
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
		for (int i = 0; i < arr.length; i++) {
			if (eq.test(e, arr[i])) {
				return i;
			}
		}
		return -1;
	}

	public static <A> A arrayFindFirst(A[] arr, Predicate<A> test) {
		if (arr == null || arr.length == 0)
			return null;
		for (int i = 0; i < arr.length; i++)
			if (test.test(arr[i]))
				return arr[i];
		return null;
	}

	public static <A> boolean arrayContains(A[] arr, Predicate<A> test) {
		return arrayFindFirst(arr, test) != null;
	}

	public static <A> boolean arrayContains(A[] arr, A e, Eq<A> eq) {
		if (arr == null || arr.length == 0)
			return false;
		for (int i = 0; i < arr.length; i++)
			if (eq.test(arr[i], e))
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

	/* Classes */

	/**
	 * @return the given object made accessible.
	 */
	public static <T extends AccessibleObject> T accessible(T obj) {
		obj.setAccessible(true);
		return obj;
	}

	/**
	 * @return A {@link Class} counts as "virtual" when it is known that its not
	 *         a type handled by an {@link Injector} context, either because it
	 *         cannot be constructed at all or it does not make sense to let the
	 *         {@link Injector} take care of it. This includes value types,
	 *         enums, collection types (including arrays) or any type than
	 *         cannot be instantiated by its nature (abstract types).
	 *
	 *         Note that this method just covers JRE types.
	 */
	public static boolean isClassVirtual(Class<?> cls) {
		return cls == null || cls.isInterface() || cls.isEnum()
			|| cls.isAnnotation() || cls.isAnonymousClass() || cls.isPrimitive()
			|| cls.isArray() || Modifier.isAbstract(cls.getModifiers())
			|| cls == String.class || Number.class.isAssignableFrom(cls)
			|| cls == Boolean.class || cls == Void.class || cls == Class.class
			|| Collection.class.isAssignableFrom(cls)
			|| Map.class.isAssignableFrom(cls);
	}

	/**
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
		for (Field f : cls.getDeclaredFields()) {
			if (!Modifier.isStatic(f.getModifiers()))
				return false;
		}
		for (Constructor<?> c : cls.getDeclaredConstructors()) {
			// maybe arguments are passed to super-type so we check it too
			if (c.getParameterTypes().length > 0)
				return isClassMonomodal(cls.getSuperclass());
		}
		return true;
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
}
