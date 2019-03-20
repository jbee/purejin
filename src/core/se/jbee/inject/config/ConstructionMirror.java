/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.function.IntPredicate;

import se.jbee.inject.Packages;
import se.jbee.inject.Type;
import se.jbee.inject.Utils;

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
	//TODO maybe use the list of constructors as parameter to allow better filtering

	/**
	 * Default value and starting point for custom {@link ConstructionMirror}.
	 */
	ConstructionMirror common = Utils::commonConstructorOrNull;

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

	default ConstructionMirror withModifier(IntPredicate filter) {
		//TODO add a filter the others can be based upon
		return null;
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

}