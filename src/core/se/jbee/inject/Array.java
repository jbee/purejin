/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static java.lang.System.arraycopy;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Library's array utility.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Array { //TODO rename to Utils and collect all lang level utility functions 

	public static <T> T[] filter(T[] array, Predicate<T> filter) {
		if (array == null || array.length == 0)
			return array;
		@SuppressWarnings("unchecked")
		T[] filtered = (T[]) newInstance(array.getClass().getComponentType(),
				array.length);
		int j = 0;
		for (int i = 0; i < array.length; i++)
			if (filter.test(array[i]))
				filtered[j++] = array[i];
		return j == array.length ? array : Arrays.copyOf(filtered, j);
	}

	public static <T> T[] append(T[] array, T value) {
		T[] copy = Arrays.copyOf(array, array.length + 1);
		copy[array.length] = value;
		return copy;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] prepand(T value, T[] array) {
		T[] copy = (T[]) newInstance(array.getClass().getComponentType(),
				array.length + 1);
		arraycopy(array, 0, copy, 1, array.length);
		copy[0] = value;
		return copy;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] newInstance(Class<T> componentType, int length) {
		return (T[]) java.lang.reflect.Array.newInstance(componentType, length);
	}

	public static <T> T[] array(Collection<? extends T> list, Class<T> type) {
		return list.toArray(newInstance(type, list.size()));
	}

	public static <T> T first(T[] array, Predicate<T> test) {
		if (array == null || array.length == 0)
			return null;
		for (int i = 0; i < array.length; i++)
			if (test.test(array[i]))
				return array[i];
		return null;
	}

	public static <T> boolean contains(T[] array, T e, BiPredicate<T, T> test) {
		if (array == null || array.length == 0)
			return false;
		for (int i = 0; i < array.length; i++)
			if (test.test(array[i], e))
				return true;
		return false;
	}

}
