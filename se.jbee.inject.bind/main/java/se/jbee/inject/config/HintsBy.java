/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

import se.jbee.inject.Dependency;
import se.jbee.inject.Hint;
import se.jbee.inject.Name;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.lang.Type.parameterType;

/**
 * Extracts the {@link Hint} hints used to resolve the {@link Dependency}s
 * of a {@link Method} or {@link Constructor} being injected.
 *
 * @since 8.1
 */
@FunctionalInterface
public interface HintsBy {

	/**
	 * @return The {@link Hint} for the given {@link Parameter}.
	 */
	Hint<?> reflect(Parameter param);

	default Hint<?>[] applyTo(Executable obj) {
		if (obj.getParameterCount() == 0)
			return Hint.none();
		Hint<?>[] hints = new Hint[obj.getParameterCount()];
		int hinted = 0;
		int i = 0;
		for (Parameter p : obj.getParameters()) {
			Hint<?> hint = reflect(p);
			if (hint == null) {
				hint = Hint.relativeReferenceTo(parameterType(p));
			} else {
				hinted++;
			}
			hints[i++] = hint;
		}
		return hinted > 0 ? hints : Hint.none();
	}

	/**
	 * A {@link HintsBy} that allows to specify the
	 * {@link Annotation} which is used to indicate the instance {@link Name} of
	 * a method parameter.
	 */
	static HintsBy instanceReference(NamesBy namesBy) {
		return param -> {
			Name name = namesBy.reflect(param);
			return name != null && !name.isDefault()
					? Hint.relativeReferenceTo(instance(name, parameterType(param)))
					: null;
		};
	}
}
