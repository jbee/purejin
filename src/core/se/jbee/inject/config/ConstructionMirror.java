/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.arrayContains;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

import se.jbee.inject.Packages;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency.NoMethodForDependency;

@FunctionalInterface
public interface ConstructionMirror {

	/**
	 * Picks the {@link Constructor} to use to construct objects of a given
	 * {@link Class}.
	 * 
	 * @return The {@link Constructor} considered to be the reasonable or right
	 *         way to construct a object of the given type. In case one with
	 *         parameters is returned the these are solved (injected).
	 * 
	 *         Returns {@code null} when no suitable constructor was found.
	 */
	<T> Constructor<T> reflect(Class<T> type);

	/**
	 * Default value and starting point for custom {@link ConstructionMirror}.
	 */
	ConstructionMirror mostParams = ConstructionMirror::mostParamsConstructorOrNull;

	default ConstructionMirror in(Packages filter) {
		return new ConstructionMirror() {

			@Override
			public <T> Constructor<T> reflect(Class<T> type) {
				return filter.contains(Type.raw(type))
					? this.reflect(type)
					: null;
			}
		};
	}

	default ConstructionMirror annotatedWith(
			Class<? extends Annotation> marker) {
		return new ConstructionMirror() {

			@SuppressWarnings("unchecked")
			@Override
			public <T> Constructor<T> reflect(Class<T> type) {
				Constructor<?>[] cs = type.getDeclaredConstructors();
				for (Constructor<?> c : cs) {
					if (c.isAnnotationPresent(marker))
						return (Constructor<T>) c;
				}
				return cs.length == 1
					? (Constructor<T>) cs[0]
					: this.reflect(type);
			}
		};
	}

	/**
	 * Returns the constructor usually should be used.
	 *
	 * @param type constructed type
	 * @return The constructor with the most parameters that does not have the
	 *         declaring class itself as parameter type (some compiler seam to
	 *         generate such a synthetic constructor)
	 * @throws NoMethodForDependency in case the type is not constructible (has
	 *             no constructors at all)
	 */
	public static <T> Constructor<T> mostParamsConstructor(Class<T> type)
			throws NoMethodForDependency {
		Constructor<?>[] cs = type.getDeclaredConstructors();
		if (cs.length == 0)
			throw new NoMethodForDependency(raw(type));
		Constructor<?> mostParamsConstructor = null;
		for (Constructor<?> c : cs) {
			if (!arrayContains(c.getParameterTypes(), type, (a, b) -> a == b) // avoid self referencing constructors (synthetic) as they cause endless loop
				&& (mostParamsConstructor == null
					|| c.getParameterCount() > mostParamsConstructor.getParameterCount())) {
				mostParamsConstructor = c;
			}
		}
		if (mostParamsConstructor == null)
			throw new NoMethodForDependency(raw(type));
		@SuppressWarnings("unchecked")
		Constructor<T> c = (Constructor<T>) mostParamsConstructor;
		return c;
	}

	public static <T> Constructor<T> mostParamsConstructorOrNull(
			Class<T> type) {
		try {
			return mostParamsConstructor(type);
		} catch (RuntimeException e) {
			return null;
		}
	}

	public static <T> Constructor<T> noArgsConstructor(Class<T> type) {
		if (type.isInterface())
			throw new NoMethodForDependency(raw(type));
		try {
			return type.getDeclaredConstructor();
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new RuntimeException(e);
		}
	}

}