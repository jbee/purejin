/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.binder;

import se.jbee.inject.Hint;
import se.jbee.inject.Injector;
import se.jbee.inject.bind.ValueBinder;
import se.jbee.inject.config.HintsBy;
import se.jbee.lang.Type;

import java.lang.reflect.Method;

import static se.jbee.lang.Type.parameterType;
import static se.jbee.lang.Utils.arrayPrepend;

/**
 * A {@link Produces} is the {@link ValueBinder} expansion wrapper for a method
 * bound to a particular instance (if not static) that produces instances to
 * inject.
 *
 * @param <T> type of the value yield by the factory method
 */
public final class Produces<T> extends ReflectiveDescriptor<Method, T> {

	public static <T> Produces<? extends T> produces(Type<T> expectedType,
			Object owner, Method target, HintsBy strategy, Hint<?>... args) {
		@SuppressWarnings("unchecked")
		Type<? extends T> actualType = (Type<? extends T>) actualType(owner, target);
		checkBasicCompatibility(Type.returnType(target), actualType, target);
		return new Produces<>(expectedType, actualType, owner, target, strategy,
				args);
	}

	public static <T> Produces<?> produces(Object owner, Method target,
			HintsBy strategy, Hint<?>... explicitHints) {
		@SuppressWarnings("unchecked")
		Type<T> actualType = (Type<T>) actualType(owner, target);
		checkBasicCompatibility(Type.returnType(target), actualType, target);
		return new Produces<>(actualType, actualType, owner, target, strategy,
				explicitHints);
	}

	private Produces(Type<? super T> expectedType, Type<T> actualType,
			Object owner, Method target, HintsBy strategy, Hint<?>[] explicitHints) {
		super(expectedType, actualType, owner, target, strategy, explicitHints);
		checkConsistentExplicitHints(target.getParameters());
	}

	private static Type<?> actualType(Object owner, Method target) {
		return requiresActualReturnType(target, Method::getReturnType,
				Method::getAnnotatedReturnType) //
				? Type.actualReturnType(target,	actualDeclaringType(owner, target)) //
				: Type.returnType(target);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> Produces<E> typed(Type<E> supertype) {
		type().castTo(supertype); // make sure is valid
		return (Produces<E>) this;
	}

	/**
	 * @return true, if the called {@link Method} uses type variables that need
	 * replacement from the actual call {@link se.jbee.inject.Dependency}.
	 */
	public boolean isGeneric() {
		return target.getTypeParameters().length > 0;
	}

	/**
	 * If a {@link Method} has a {@link Type} parameter as its first parameter
	 * we assume it is meant to be the actual type for a type variable used by
	 * the {@link #target} method.
	 *
	 * @return true if the target {@link Method} has a matching {@link Type}
	 * parameter as it first parameter, otherwise false
	 */
	public boolean isGenericTypeAware() {
		if (!isGeneric())
			return false;
		Type<?> arg0Type = parameterType(target.getParameters()[0]);
		if (arg0Type.rawType != Type.class)
			return false;
		return arg0Type.parameter(0).equalTo(type());
	}

	public Hint<?>[] actualParameters(Type<?> actualDeclaringType, Injector context) {
		Hint<?>[] given = explicitHints;
		if (isGenericTypeAware()) {
			// use a constant null hint to blank first parameter as it is filled in with actual type on method invocation
			Hint<?> actualTypeHint = Hint.constantNull(
					Type.parameterType(target.getParameters()[0]));
			given = arrayPrepend(actualTypeHint, given);
		}
		return strategy.applyTo(context, target, actualDeclaringType, given);
	}
}
