/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

import se.jbee.inject.Hint;
import se.jbee.inject.lang.Utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Predicate;

import static se.jbee.inject.lang.Utils.arrayFilter;

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
	Constructor<?> reflect(Constructor<?>[] candidates, Hint<?>... hints);

	ConstructsBy FIRST = (candidates, hints) -> candidates.length == 0 ? null : candidates[0];

	/**
	 * Is the {@link Constructor} that is most visible and out of those has the
	 * most parameters but does not have a parameter that is the type itself (as
	 * this would usually cause an endless loop when trying to inject it).
	 */
	ConstructsBy OPTIMISTIC = FIRST
			.ignore(Utils::isRecursiveTypeParameterPresent) //
			.sortedBy(Utils::mostVisibleMostParametersToLeastVisibleLeastParameters);

	default ConstructsBy sortedBy(Comparator<Constructor<?>> cmp) {
		return (candidates, hints) -> {
			if (candidates.length == 0) return null;
			if (candidates.length == 1) return candidates[0];
			Arrays.sort(candidates, cmp);
			return reflect(candidates, hints);
		};
	}

	default ConstructsBy ignore(Predicate<Constructor<?>> filter) {
		return select(filter.negate());
	}

	default ConstructsBy select(Predicate<Constructor<?>> filter) {
		return (candidates, hints) -> candidates.length == 0
				? null
				: reflect(arrayFilter(candidates, filter), hints);
	}

	default ConstructsBy annotatedWith(Class<? extends Annotation> marker) {
		return select(c -> c.isAnnotationPresent(marker));
	}
}
