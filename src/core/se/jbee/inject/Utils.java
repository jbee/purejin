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
import java.util.function.Predicate;

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

	public static <T> T[] arrayFilter(T[] arr, Predicate<T> accept) {
		if (arr == null || arr.length == 0)
			return arr;
		T[] accepted = newArray(arr, arr.length);
		int j = 0;
		for (int i = 0; i < arr.length; i++)
			if (accept.test(arr[i]))
				accepted[j++] = arr[i];
		return j == arr.length ? arr : copyOf(accepted, j);
	}

	public static <T> T[] arrayAppend(T[] arr, T e) {
		T[] copy = copyOf(arr, arr.length + 1);
		copy[arr.length] = e;
		return copy;
	}

	public static <T> T[] arrayPrepand(T e, T[] arr) {
		T[] copy = newArray(arr, arr.length + 1);
		arraycopy(arr, 0, copy, 1, arr.length);
		copy[0] = e;
		return copy;
	}

	public static <T> T[] arrayInsert(T e, T[] arr, Eq<T> eq) {
		if (arr.length == 0)
			return arrayPrepand(e, arr);
		int i = arrayIndex(arr, e, eq);
		if (i >= 0) {
			if (e == arr[i]) // already very same
				return arr;
			T[] tmp = arr.clone();
			tmp[i] = e;
			return tmp;
		}
		return arrayPrepand(e, arr);
	}

	public static <T> int arrayIndex(T[] arr, T e, Eq<T> eq) {
		if (arr == null || arr.length == 0)
			return -1;
		for (int i = 0; i < arr.length; i++) {
			if (eq.test(e, arr[i])) {
				return i;
			}
		}
		return -1;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] newArray(T[] proto, int length) {
		return (T[]) newArray(proto.getClass().getComponentType(), length);
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] newArray(Class<T> componentType, int length) {
		return (T[]) newInstance(componentType, length);
	}

	public static <T> T[] arrayOf(Collection<? extends T> list, Class<T> type) {
		return list.toArray(newArray(type, list.size()));
	}

	public static <T> T arrayFirst(T[] arr, Predicate<T> test) {
		if (arr == null || arr.length == 0)
			return null;
		for (int i = 0; i < arr.length; i++)
			if (test.test(arr[i]))
				return arr[i];
		return null;
	}

	public static <T> boolean arrayContains(T[] arr, T e, Eq<T> eq) {
		if (arr == null || arr.length == 0)
			return false;
		for (int i = 0; i < arr.length; i++)
			if (eq.test(arr[i], e))
				return true;
		return false;
	}

	public static Method annotationPropertyByType(Class<?> type,
			Class<? extends Annotation> annotation) {
		return arrayFirst(annotation.getDeclaredMethods(),
				m -> m.getReturnType() == type);
	}

	/**
	 * @return the given object made accessible.
	 */
	public static <T extends AccessibleObject> T accessible(T obj) {
		obj.setAccessible(true);
		return obj;
	}

	/**
	 * @return A {@link Class} is "virtual" when there is no typical "injection"
	 *         style way to create instances. This is true for all value types,
	 *         enums, collection types (including arrays) or any type than
	 *         cannot be instantiated by its nature (abstract types).
	 *
	 *         Note that this method just covers those types that are *known* to
	 *         be "virtual". There will be a lot of user defined types that are
	 *         "virtual" as well but which will not return true.
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
			if (!Modifier.isStatic(f.getModifiers())) {
				return false;
			}
		}
		for (Constructor<?> c : cls.getDeclaredConstructors()) {
			if (c.getParameterTypes().length > 0) {
				// maybe arguments are passed to super-type so we check it too
				return isClassMonomodal(cls.getSuperclass());
			}
		}
		return true;
	}

}
