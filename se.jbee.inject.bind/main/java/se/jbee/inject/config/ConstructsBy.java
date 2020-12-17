/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

import static se.jbee.inject.lang.Utils.arrayFindFirst;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

import se.jbee.inject.Hint;
import se.jbee.inject.Packages;
import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.Utils;

/**
 * Picks the {@link Constructor} to use to construct objects of a given
 * {@link Class}.
 *
 * @since 8.1
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
	Constructor<?> reflect(Class<?> type, Hint... hints);

	/**
	 * Default value and starting point for custom {@link ConstructsBy}.
	 */
	ConstructsBy common = (type, hints) -> Utils.commonConstructorOrNull(type);

	default ConstructsBy in(Packages filter) {
		return (type, hints) -> filter.contains(Type.raw(type)) ? reflect(type) : null;
	}

	default ConstructsBy annotatedWith(Class<? extends Annotation> marker) {
		return (type, hints) -> {
			Constructor<?> marked = arrayFindFirst(type.getDeclaredConstructors(),
					c -> c.isAnnotationPresent(marker));
			return marked != null ? marked : reflect(type);
		};
	}
}
