/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

import se.jbee.inject.*;
import se.jbee.inject.lang.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.lang.Type.actualParameterType;
import static se.jbee.inject.lang.Type.parameterType;
import static se.jbee.inject.lang.Utils.arrayMap;

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

	HintsBy AUTO = param -> null;

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
	default Hint<?>[] applyTo(Executable obj, Type<?> genericDeclaringClass,
			Hint<?>... determined) {
		if (obj.getParameterCount() == 0)
			return Hint.none();
		Parameter[] params = obj.getParameters();
		Hint<?>[] hints = new Hint[params.length];
		Type<?>[] types = arrayMap(params, Type.class,
				p -> actualParameterType(p, genericDeclaringClass));
		// find position for the already given hints
		for (Hint<?> hint : determined) {
			int i = Hint.indexForType(types, hint, hints);
			if (i < 0)
				throw InconsistentDeclaration.incomprehensibleHint(hint);
			hints[i] = hint.parameterized(types[i]);
		}
		// fill parameters without hints by either HintsBy or rel. type reference as default
		for (int i = 0; i < hints.length; i++)
			if (hints[i] == null) {
				Hint<?> hint = reflect(params[i]);
				hints[i] = hint != null
						? hint.parameterized(types[i])
						: Hint.relativeReferenceTo(types[i]);
			}
		return hints;
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
