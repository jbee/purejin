/*
 *  Copyright (c) 2012-2019, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static java.lang.System.arraycopy;

import java.util.Arrays;
import java.util.Collection;

/**
 * Library's array utility.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Array {

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

}
