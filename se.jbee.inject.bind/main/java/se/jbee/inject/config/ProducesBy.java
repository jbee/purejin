/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

import se.jbee.inject.Hint;
import se.jbee.inject.Packages;
import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.Utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import static se.jbee.inject.lang.Type.raw;
import static se.jbee.inject.lang.Type.returnType;
import static se.jbee.inject.lang.Utils.arrayFilter;

/**
 * Extracts the relevant {@link Method}s for a given target {@link Class}. These
 * can be methods for different purposes. For example factory methods or methods
 * that are used in advanced abstractions like actions.
 *
 * @since 8.1
 */
@FunctionalInterface
public interface ProducesBy {

	ProducesBy OPTIMISTIC = declaredMethods(m -> !m.isSynthetic(), true);

	/**
	 * @return The {@link Method}s that should be used in the context this
	 * {@link ProducesBy} is used. Return {@code null} when no decision has been
	 * made. Returns empty array when the decision is to not use any producer
	 * methods.
	 */
	Method[] reflect(Class<?> impl);

	static ProducesBy declaredMethods(boolean recursive) {
		return declaredMethods(null, recursive);
	}

	static ProducesBy declaredMethods(Predicate<Method> filter, boolean recursive) {
		return methods(Class::getDeclaredMethods, filter, recursive);
	}

	static ProducesBy methods(Function<Class<?>, Method[]> selection,
			Predicate<Method> filter, boolean recursive) {
		return impl -> arrayFilter(impl,
				recursive ? Object.class : impl.getSuperclass(), selection,
				filter).toArray(Method[]::new);
	}

	static ProducesBy methods(Function<Class<?>, Method[]> selection,
			Predicate<Method> filter, Class<?> top) {
		return impl -> arrayFilter(impl, top, selection, filter)
				.toArray(Method[]::new);
	}

	default ProducesBy orElse(ProducesBy whenNull) {
		return impl -> {
			Method[] res = reflect(impl);
			return res != null ? res : whenNull.reflect(impl);
		};
	}

	default ProducesBy and(ProducesBy other) {
		return impl -> Utils.arrayConcat(reflect(impl), other.reflect(impl));
	}

	default ProducesBy ignoreStatic() {
		return withModifier(((IntPredicate) Modifier::isStatic).negate());
	}

	default ProducesBy ignoreSynthetic() {
		return select(method -> !method.isSynthetic());
	}

	default ProducesBy ignoreGenericReturnType() {
		return select(
				method -> !(method.getGenericReturnType() instanceof TypeVariable<?>));
	}

	default ProducesBy ignore(Predicate<Method> filter) {
		return select(filter.negate());
	}

	default ProducesBy select(Predicate<Method> filter) {
		return impl -> arrayFilter(reflect(impl), filter);
	}

	default ProducesBy selectBy(Hint<?>... hints) {
		return select(method -> Hint.matches(method, hints));
	}

	default ProducesBy returnTypeAssignableTo(Type<?> supertype) {
		return select(method -> returnType(method).isAssignableTo(supertype));
	}

	default ProducesBy withModifier(IntPredicate filter) {
		return select(method -> filter.test(method.getModifiers()));
	}

	default ProducesBy annotatedWith(Class<? extends Annotation> marker) {
		return select(method -> method.isAnnotationPresent(marker));
	}

	default ProducesBy returnTypeIn(Packages filter) {
		return select(method -> filter.contains(raw(method.getReturnType())));
	}

	default ProducesBy in(Packages filter) {
		return impl -> filter.contains(raw(impl))
			? reflect(impl)
			: null;
	}

	default ProducesBy in(Class<?> api) {
		return select(method -> raw(method.getDeclaringClass()).isAssignableTo(raw(api)));
	}
}
