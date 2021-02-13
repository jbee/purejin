/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

import se.jbee.inject.Packages;
import se.jbee.lang.Typed;
import se.jbee.lang.Utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static se.jbee.lang.Type.raw;
import static se.jbee.lang.Type.returnType;
import static se.jbee.lang.Utils.arrayFilter;

/**
 * Extracts the relevant {@link Method}s for a given target {@link Class}. These
 * can be methods for different purposes. For example factory methods or methods
 * that are used in advanced abstractions like actions.
 *
 * @since 8.1
 */
@FunctionalInterface
public interface ProducesBy {

	/**
	 * The default pool of methods contains all non synthetic {@link Method}
	 * including all inherited ones except for those declared (and not
	 * overridden) in {@link Object}.
	 */
	ProducesBy OPTIMISTIC = declaredMethods(m -> !m.isSynthetic(), true);

	/**
	 * @return The {@link Method}s that should be used in the context this
	 * {@link ProducesBy} is used. Return {@code null} when no decision has been
	 * made. Returns empty array when the decision is to not use any producer
	 * methods.
	 */
	Method[] reflect(Class<?> impl);

	static ProducesBy declaredMethods(boolean includeInherited) {
		return declaredMethods(null, includeInherited);
	}

	static ProducesBy declaredMethods(Predicate<Method> filter,
			boolean includeInherited) {
		return methods(Class::getDeclaredMethods, filter, includeInherited);
	}

	static ProducesBy methods(Function<Class<?>, Method[]> pool,
			Predicate<Method> filter, boolean includeInherited) {
		return methods(pool, filter,
				impl -> includeInherited ? Object.class : impl.getSuperclass());
	}

	static ProducesBy methods(Function<Class<?>, Method[]> pool,
			Predicate<Method> filter, UnaryOperator<Class<?>> end) {
		return impl -> arrayFilter(impl, end.apply(impl), pool, filter)
				.toArray(new Method[0]);
	}

	default ProducesBy orElse(ProducesBy whenNull) {
		return impl -> {
			Method[] res = reflect(impl);
			return res != null ? res : whenNull.reflect(impl);
		};
	}

	default ProducesBy or(ProducesBy other) {
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

	default ProducesBy parameterTypesStrictlyMatch(Typed<?>... hints) {
		return select(method -> Utils.matchesInOrder(method, hints));
	}

	default ProducesBy parameterTypesMatch(Typed<?>... hints) {
		return select(method -> Utils.matchesInRandomOrder(method, hints));
	}

	default ProducesBy returnTypeAssignableTo(Typed<?> supertype) {
		return select(method -> returnType(method).isAssignableTo(supertype.type()));
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
		return select(method -> raw(method.getDeclaringClass()) //
				.isAssignableTo(raw(api)));
	}
}
