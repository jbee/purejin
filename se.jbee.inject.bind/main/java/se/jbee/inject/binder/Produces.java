/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.binder;

import se.jbee.inject.Hint;
import se.jbee.inject.bind.ValueBinder;
import se.jbee.inject.lang.Type;

import java.lang.reflect.Method;

import static se.jbee.inject.lang.Type.parameterType;

/**
 * A {@link Produces} is the {@link ValueBinder} expansion wrapper for a method
 * bound to a particular instance (if not static) that produces instances to
 * inject.
 *
 * @param <T> type of the value yield by the factory method
 */
public final class Produces<T> extends ReflectiveDescriptor<Method, T> {

	public static <T> Produces<? extends T> produces(Type<T> expectedType,
			Object owner, Method target, Hint<?>... args) {
		@SuppressWarnings("unchecked")
		Type<? extends T> actualType = (Type<? extends T>) actualType(owner, target);
		checkBasicCompatibility(Type.returnType(target), actualType, target);
		return new Produces<>(expectedType, actualType, owner, target, args);
	}

	public static <T> Produces<?> produces(Object owner, Method target,
			Hint<?>... args) {
		@SuppressWarnings("unchecked")
		Type<T> actualType = (Type<T>) actualType(owner, target);
		checkBasicCompatibility(Type.returnType(target), actualType, target);
		return new Produces<>(actualType, actualType, owner, target, args);
	}

	private Produces(Type<? super T> expectedType, Type<T> actualType,
			Object owner, Method target, Hint<?>[] hints) {
		super(expectedType, owner, target, hints, actualType);
	}

	private static Type<?> actualType(Object owner, Method target) {
		return requiresActualReturnType(target, Method::getReturnType,
				Method::getAnnotatedReturnType) //
				? Type.actualReturnType(target,	actualDeclaringType(owner, target)) //
				: Type.returnType(target);
	}

	public boolean declaresTypeParameters() {
		return target.getTypeParameters().length > 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> Produces<E> typed(Type<E> supertype) {
		type().castTo(supertype); // make sure is valid
		return (Produces<E>) this;
	}

	/**
	 * If a {@link Method} has a {@link Type} parameter as its first parameter
	 * we assume it is meant to be the actual type.
	 *
	 * @return true if the target {@link Method} has a matching {@link Type}
	 * parameter as it first parameter, otherwise false
	 */
	public boolean usesActualTypeFirstParameter() {
		if (target.getParameterCount() == 0)
			return false;
		Type<?> arg0Type = parameterType(target.getParameters()[0]);
		if (arg0Type.rawType != Type.class)
			return false;
		return arg0Type.parameter(0).equalTo(type());
	}
}
