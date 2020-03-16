/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

import static se.jbee.inject.Utils.arrayFindFirst;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

import se.jbee.inject.Packages;
import se.jbee.inject.Type;
import se.jbee.inject.Utils;

/**
 * Picks the {@link Constructor} to use to construct objects of a given
 * {@link Class}.
 * 
 * @since 19.1
 */
@FunctionalInterface
public interface ConstructsBy {

	/**
	 * @return The {@link Constructor} considered to be the reasonable or right
	 *         way to construct a object of the given type. In case one with
	 *         parameters is returned the these are solved (injected).
	 * 
	 *         Returns {@code null} when no suitable constructor was found.
	 */
	<T> Constructor<T> reflect(Class<T> type);

	/**
	 * Default value and starting point for custom {@link ConstructsBy}.
	 */
	ConstructsBy common = Utils::commonConstructorOrNull;

	default ConstructsBy in(Packages filter) {
		return new ConstructsBy() {

			@Override
			public <T> Constructor<T> reflect(Class<T> type) {
				return filter.contains(Type.raw(type))
					? this.reflect(type)
					: null;
			}
		};
	}

	default ConstructsBy annotatedWith(
			Class<? extends Annotation> marker) {
		return new ConstructsBy() {

			@Override
			public <T> Constructor<T> reflect(Class<T> type) {
				@SuppressWarnings("unchecked")
				Constructor<T>[] cs = (Constructor<T>[]) type.getDeclaredConstructors();
				Constructor<T> marked = arrayFindFirst(cs,
						c -> c.isAnnotationPresent(marker));
				return marked != null ? marked : this.reflect(type);
			}
		};
	}

}