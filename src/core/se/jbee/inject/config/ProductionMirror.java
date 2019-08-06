/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

import static java.util.Arrays.asList;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Type.returnType;
import static se.jbee.inject.Utils.arrayFilter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import se.jbee.inject.Packages;
import se.jbee.inject.Type;

/**
 * Extracts the relevant {@link Method}s for a given target {@link Class}. These
 * can be methods for different purposes. For example factory methods or methods
 * that are used in advanced abstractions like actions.
 * 
 * @since 19.1
 */
@FunctionalInterface
public interface ProductionMirror {

	Method[] __noMethodsArray = new Method[0];

	/**
	 * @return The {@link Member}s that should be used in the context this
	 *         {@link ProductionMirror} is used.
	 */
	Method[] reflect(Class<?> impl);

	ProductionMirror noMethods = impl -> __noMethodsArray;
	ProductionMirror declaredMethods = ((ProductionMirror) Class::getDeclaredMethods).ignoreSynthetic();
	ProductionMirror allMethods = ((ProductionMirror) ProductionMirror::allMethods).ignoreSynthetic();

	default ProductionMirror ignoreStatic() {
		return withModifier(((IntPredicate) Modifier::isStatic).negate());
	}

	default ProductionMirror ignoreSynthetic() {
		return select(method -> !method.isSynthetic());
	}

	default ProductionMirror ignoreGenericReturnType() {
		return select(
				method -> !(method.getGenericReturnType() instanceof TypeVariable<?>));
	}

	default ProductionMirror ignore(Predicate<Method> filter) {
		return select(filter.negate());
	}

	default ProductionMirror select(Predicate<Method> filter) {
		return impl -> arrayFilter(this.reflect(impl), filter);
	}

	default ProductionMirror returnTypeAssignableTo(Type<?> supertype) {
		return select(method -> returnType(method).isAssignableTo(supertype));
	}

	default ProductionMirror withModifier(IntPredicate filter) {
		return select(method -> filter.test(method.getModifiers()));
	}

	default ProductionMirror annotatedWith(Class<? extends Annotation> marker) {
		return select(method -> method.isAnnotationPresent(marker));
	}

	default ProductionMirror returnTypeIn(Packages filter) {
		return select(method -> filter.contains(raw(method.getReturnType())));
	}

	default ProductionMirror in(Packages filter) {
		return impl -> filter.contains(raw(impl))
			? this.reflect(impl)
			: __noMethodsArray;
	}

	static Method[] allMethods(Class<?> type) {
		List<Method> all = new ArrayList<>();
		while (type != Object.class && type != null) {
			all.addAll(asList(type.getDeclaredMethods()));
			type = type.getSuperclass();
		}
		return all.toArray(new Method[0]);
	}
}
