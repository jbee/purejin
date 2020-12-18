/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

import se.jbee.inject.Dependency;
import se.jbee.inject.Hint;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.lang.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.lang.Type.parameterType;

/**
 * Extracts the {@link Hint} used to resolve the {@link Dependency}s for a
 * {@link Method} or {@link Constructor} {@link Parameter}.
 * <p>
 * The binder API uses {@link #applyTo(Executable)} to resolve the {@link Hint}s
 * for all {@link Parameter}s. By convention this uses a {@link
 * Hint#relativeReferenceTo(Type)} for those {@link Parameter}s where the {@link
 * HintsBy} strategy did return {@code null}. If {@code null} was returned for
 * all {@link Parameter}s no {@link Hint}s will be used.
 *
 * @see NamesBy
 *
 * @since 8.1
 */
@FunctionalInterface
public interface HintsBy {

	/**
	 * @return The {@link Hint} for the given {@link Parameter}.
	 */
	Hint<?> reflect(Parameter param);

	/**
	 * Used by the binder API to resolve {@link Hint}s for an {@link
	 * Executable}.
	 *
	 * @param obj the {@link Method} or {@link Constructor} to hint for
	 * @return the {@link Hint}s to use, zero length array for no hints
	 */
	default Hint<?>[] applyTo(Executable obj) {
		if (obj.getParameterCount() == 0)
			return Hint.none();
		Hint<?>[] hints = new Hint[obj.getParameterCount()];
		int hinted = 0;
		int i = 0;
		for (Parameter p : obj.getParameters()) {
			Hint<?> hint = reflect(p);
			if (hint == null) {
				// prevent misunderstanding a later given hint for another parameter
				hint = Hint.relativeReferenceTo(parameterType(p));
			} else {
				hinted++;
			}
			hints[i++] = hint;
		}
		return hinted > 0 ? hints : Hint.none();
	}

	default HintsBy orElse(HintsBy whenNull) {
		return param -> {
			Hint<?> hint = reflect(param);
			return hint != null ? hint : whenNull.reflect(param);
		};
	}

	/**
	 * Returns a strategy that return {@link Hint#relativeReferenceTo(Instance)}
	 * in case the provided {@link NamesBy} strategy does return a {@link
	 * Name}.
	 */
	static HintsBy instanceReference(NamesBy namesBy) {
		return param -> {
			Name name = namesBy.reflect(param);
			return name != null
					? Hint.relativeReferenceTo(instance(name, parameterType(param)))
					: null;
		};
	}
}
