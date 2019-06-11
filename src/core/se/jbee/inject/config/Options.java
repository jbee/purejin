/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

import java.util.HashMap;
import java.util.Map;

import se.jbee.inject.Type;

/**
 * {@link Options} are an immutable associative data structure associating a
 * exact {@link Type} (including generics) with a value for/of that exact given
 * type. These values act as input <i>parameters</i> to the bootstrapping
 * process. The values are used within modules that depend on data that is given
 * as program input.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Options {

	public static final Options NONE = new Options(new HashMap<>(0));

	private final Map<String, Object> values;

	private Options(Map<String, Object> values) {
		this.values = values;
	}

	/**
	 * @see #set(Type, Object)
	 */
	public <T> Options set(Class<T> type, T value) {
		return set(Type.raw(type), value);
	}

	/**
	 * @return new {@link Options} instance with the given key-value
	 *         association. Any existing association of the same key will be
	 *         overridden.
	 */
	public <T> Options set(Type<T> type, T value) {
		final String key = key(type);
		if (value == null && !values.containsKey(key))
			return this;
		Map<String, Object> clone = new HashMap<>(values);
		if (value == null) {
			clone.remove(key);
		} else {
			clone.put(key, value);
		}
		return new Options(clone);
	}

	/**
	 * @return The value associated with the given exact {@link Type} or
	 *         <code>null</code> of no value is associated with it.
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(Type<T> type) {
		return (T) values.get(key(type));
	}

	private static <T> String key(Type<T> type) {
		return type.toString().intern();
	}

	@Override
	public String toString() {
		return values.toString();
	}
}
