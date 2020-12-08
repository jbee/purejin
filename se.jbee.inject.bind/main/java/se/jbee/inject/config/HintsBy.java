/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

import static se.jbee.inject.InconsistentDeclaration.annotationLacksProperty;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.lang.Type.parameterTypes;
import static se.jbee.inject.lang.Utils.annotationPropertyByType;
import static se.jbee.inject.lang.Utils.arrayFindFirst;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;

import se.jbee.inject.Dependency;
import se.jbee.inject.Hint;
import se.jbee.inject.Name;
import se.jbee.inject.lang.Type;

/**
 * Extracts the {@link Hint} hints used to resolve the {@link Dependency}s
 * of a {@link Method} or {@link Constructor} being injected.
 *
 * @since 8.1
 */
@FunctionalInterface
public interface HintsBy {

	/**
	 * @return The {@link Hint} hints for the construction/invocation of
	 *         the given object. This is either a
	 *         {@link java.lang.reflect.Constructor} or a
	 *         {@link java.lang.reflect.Method} Use a zero length array if there
	 *         are no hits.
	 */
	Hint<?>[] reflect(Executable obj);

	HintsBy noParameters = obj -> Hint.none();

	/**
	 * A {@link HintsBy} that allows to specify the
	 * {@link Annotation} which is used to indicate the instance {@link Name} of
	 * a method parameter.
	 */
	default HintsBy unlessAnnotatedWith(
			Class<? extends Annotation> naming) {
		if (naming == null)
			return this;
		Method nameProperty = annotationPropertyByType(String.class, naming);
		if (nameProperty == null)
			throw annotationLacksProperty(String.class, naming);
		return obj -> {
			Annotation[][] ais = obj.getParameterAnnotations();
			Type<?>[] tis = parameterTypes(obj);
			Hint<?>[] res = new Hint[tis.length];
			int named = 0;
			for (int i = 0; i < res.length; i++) {
				res[i] = Hint.relativeReferenceTo(tis[i]); // default
				Annotation instance = arrayFindFirst(ais[i],
						a -> naming == a.annotationType());
				if (instance != null) {
					//TODO nicer exception handling for invoke (same in other mirrors)
					try {
						String name = (String) nameProperty.invoke(instance);
						if (!name.isEmpty()
							&& !name.equals(nameProperty.getDefaultValue())) {
							res[i] = instance(named(name), tis[i]).asHint();
							named++;
						}
					} catch (Exception e) {
						// gobble
					}
				}
			}
			return named == 0 ? this.reflect(obj) : res;

		};
	}
}
