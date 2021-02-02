/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

import se.jbee.inject.Name;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;
import java.util.function.Function;

import static se.jbee.inject.Name.named;

/**
 * A strategy to extract the {@link Name} used for instance from either the
 * {@link AccessibleObject}.
 *
 * @since 8.1
 */
@FunctionalInterface
public interface NamesBy {

	/**
	 * Uses the name as declared in the Java source code. This is the {@link
	 * Member#getName()}, the {@link Class#getSimpleName()} or the {@link
	 * Parameter#getName()}. {@link Parameter} only use the name if {@link
	 * Parameter#isNamePresent()}.
	 */
	NamesBy DECLARED_NAME = obj ->  {
		if (obj instanceof Member) return named(((Member) obj).getName());
		if (obj instanceof Class) return named(((Class<?>) obj).getSimpleName());
		if (obj instanceof Parameter) //
			return ((Parameter) obj).isNamePresent() //
					? named(((Parameter) obj).getName())
					: null;
		return null;
	};

	/**
	 * @return The {@link Name} of the instance provided by the given object.
	 * When composing null can be returned to indicate that no decision has been
	 * made. If the overall {@link NamesBy} returns null {@link Name#DEFAULT} is
	 * used.
	 */
	Name reflect(AnnotatedElement obj);

	default NamesBy orElse(Name name) {
		return obj -> {
			Name n = reflect(obj);
			return n != null ? n : name;
		};
	}

	default NamesBy orElse(NamesBy whenNull) {
		return obj -> {
			Name n = reflect(obj);
			return n != null ? n : whenNull.reflect(obj);
		};
	}

	static <T extends Annotation> NamesBy annotatedWith(Class<T> annotation,
			Function<T, String> property) {
		return annotatedWith(annotation, property, false);
	}

	static <T extends Annotation> NamesBy annotatedWith(Class<T> annotation,
			Function<T, String> property, boolean absoluteName) {
		return obj -> {
			if (!obj.isAnnotationPresent(annotation))
				return null;
			String name = property.apply(obj.getAnnotation(annotation));
			return absoluteName ? named(name).in(named(annotation)) : named(name);
		};
	}
}
