/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.lang;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.*;

import static java.lang.System.arraycopy;
import static java.lang.reflect.Array.newInstance;
import static java.lang.reflect.Modifier.*;
import static java.util.Arrays.copyOf;
import static se.jbee.lang.Type.classType;
import static se.jbee.lang.Type.parameterTypes;

/**
 * Language level utility methods for the library.
 */
@SuppressWarnings({ "squid:S1200", "squid:S1448" })
public final class Utils {

	private Utils() {
		throw new UnsupportedOperationException("util");
	}

	/**
	 * Function that determines of two elements of the same type are equal.
	 *
	 * @param <T> type of the elements compared
	 */
	@FunctionalInterface
	public interface Eq<T> extends BiPredicate<T, T> {
	}

	/* Arrays */

	public static <T> T[] arrayConcat(T[] a, T[] b) {
		if (a == null)
			return b;
		if (b == null)
			return a;
		if (a.length == 0)
			return b;
		if (b.length == 0)
			return a;
		T[] both = Arrays.copyOf(a, a.length + b.length);
		System.arraycopy(b, 0, both, a.length, b.length);
		return both;
	}

	/**
	 * This implementation assumes that the array passed is usually short (< 20)
	 * and that usually the filter does accept all elements it is the case for
	 * example with lists of {@link Member}s and filters on those.
	 *
	 * @param arr    an array or null
	 * @param accept a {@link Predicate} that returns true for those elements
	 *               that should stay
	 * @param <A>    array element type
	 * @return the filtered array, the same instance if all elements get
	 * accepted, or a new instance of some get filtered out
	 */
	public static <A> A[] arrayFilter(A[] arr, Predicate<A> accept) {
		if (arr == null || arr.length == 0)
			return arr;
		int[] acceptedIndex = new int[arr.length];
		int j = 0;
		for (int i = 0; i < arr.length; i++)
			if (accept.test(arr[i]))
				acceptedIndex[j++] = i;
		if (j == arr.length)
			return arr;
		A[] accepted = newArray(arr, j);
		for (int i = 0; i < j; i++)
			accepted[i] = arr[acceptedIndex[i]];
		return accepted;
	}

	public static <A> A[] arrayAppend(A[] arr, A e) {
		A[] copy = copyOf(arr, arr.length + 1);
		copy[arr.length] = e;
		return copy;
	}

	public static <A> A[] arrayPrepend(A e, A[] arr) {
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

	public static <A, B> List<B> arrayFilter(Class<A> root, Class<?> top,
			Function<Class<?>, B[]> map2elems, Predicate<B> filter) {
		List<B> res = new ArrayList<>();
		arrayFilter(root, top, map2elems, filter, res);
		return res;
	}

	public static <A, B> void arrayFilter(Class<A> root, Class<?> end,
			Function<Class<?>, B[]> map2elems, Predicate<B> filter,
			List<B> acc) {
		if (root == null || root == end)
			return;
		for (B e : map2elems.apply(root))
			if (filter == null || filter.test(e))
				acc.add(e);
		arrayFilter(root.getSuperclass(), end, map2elems, filter, acc);
	}

	/* Classes / Types */

	/**
	 * @param cls Any {@link Class} object, not null
	 * @return A {@link Class} counts as "virtual" when it is known that its not
	 *         a type handled by an injector context, either because it
	 *         cannot be constructed at all or it does not make sense to let the
	 *         injector take care of it. This includes value types,
	 *         enums, collection types (including arrays) or any type than
	 *         cannot be instantiated by its nature (abstract types).
	 *
	 *         Note that this method just covers JRE types.
	 */
	@SuppressWarnings({ "squid:S1067", "squid:S1541" })
	private static boolean isClassNotConstructable(Class<?> cls) {
		return cls == null || cls.isInterface() || cls.isEnum()
			|| cls.isAnnotation() || cls.isAnonymousClass() || cls.isPrimitive()
			|| cls.isArray() || isAbstract(cls.getModifiers())
			|| cls == String.class || Number.class.isAssignableFrom(cls)
			|| cls == Boolean.class || cls == Void.class || cls == Class.class;
	}

	public static boolean isClassConstructable(Class<?> cls) {
		return !isClassNotConstructable(cls);
	}

	/**
	 * @param cls Any {@link Class} object, not null
	 * @return A {@link Class} concept is stateless if it there is just a single
	 *         possible initial state. All newly created instances can just have
	 *         this similar initial state but due to internal state they could
	 *         (not necessarily must) develop (behave) different later on.
	 */
	public static boolean isClassConceptStateless(Class<?> cls) {
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
				return isClassConceptStateless(cls.getSuperclass());
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
			&& isClassConceptStateless(cls);
	}

	public static boolean isLambda(Object obj) {
		return obj != null && obj.getClass().getName().contains("$$Lambda$");
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

	public static int mostVisibleMostParametersToLeastVisibleLeastParameters(
			Constructor<?> a, Constructor<?> b) {
		return a.equals(b) ? 0 : moreVisibleMoreParameters(a, b) == b ? 1 : -1;
	}

	public static Constructor<?> moreVisibleMoreParameters(Constructor<?> a,
			Constructor<?> b) {
		if (a == null)
			return b;
		if (moreVisible(a, b) == b)
			return b;
		if (moreVisible(b, a) == a)
			return a;
		return b.getParameterCount() > a.getParameterCount() ? b : a;
	}

	public static <T> boolean isRecursiveTypeParameterPresent(Constructor<T> c) {
		Class<T> t = c.getDeclaringClass();
		return arrayContains(c.getParameterTypes(), t, Class::equals) // first check raw types
				&& arrayContains(parameterTypes(c), classType(t), se.jbee.lang.Type::equalTo); // then check full generic Type as it is much more work
	}

	public static boolean matchesInOrder(Executable member, Typed<?>[] hints) {
		if (hints.length == 0)
			return true;
		se.jbee.lang.Type<?>[] types = parameterTypes(member);
		int i = 0;
		for (Typed<?> hint : hints) {
			while (i < types.length && !hint.type().isAssignableTo(types[i]))
				i++;
			if (i >= types.length)
				return false;
		}
		return true;
	}

	public static boolean matchesInRandomOrder(Executable member, Typed<?>[] hints) {
		if (hints.length == 0)
			return true;
		se.jbee.lang.Type<?>[] types = parameterTypes(member);
		for (Typed<?> hint : hints) {
			boolean matched = false;
			int i = 0;
			while (!matched && i < types.length) {
				Type<?> type = types[i];
				if (type != null && hint.type().isAssignableTo(type)) {
					types[i] = null; // nark as handled by removing it
					matched = true;
				}
				i++;
			}
			if (!matched)
				return false;
		}
		return true;
	}

	/* Char Sequences */

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

	public static <T> T orElse(T defaultValue, Supplier<T> src) {
		try {
			return src.get();
		} catch (Exception e) {
			return defaultValue;
		}
	}

}
