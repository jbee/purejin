/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.binder;

import se.jbee.inject.Hint;
import se.jbee.inject.bind.ValueBinder;
import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.Typed;

import java.lang.reflect.Method;

import static java.lang.reflect.Modifier.isStatic;
import static se.jbee.inject.lang.Type.parameterType;
import static se.jbee.inject.lang.Type.raw;

/**
 * A {@link Produces} is the {@link ValueBinder} expansion wrapper for a method
 * bound to a particular instance (if not static) that produces instances to
 * inject.
 *
 * @param <T> type of the value yield by the factory method
 */
public final class Produces<T> implements Typed<T> {

	public static Produces<?> produces(Object owner, Method target,
			Hint<?>... hints) {
		return new Produces<>(owner, target, hints);
	}

	public final Object owner;
	public final Method target;
	public final Type<T> returns;
	public final Hint<?>[] hints;
	public final boolean isInstanceMethod;
	public final boolean hasTypeVariables;

	@SuppressWarnings("unchecked")
	private Produces(Object owner, Method target, Hint<?>[] hints) {
		this.returns = (Type<T>) Type.returnType(target);
		this.target = target;
		this.hints = hints;
		this.owner = owner;
		this.isInstanceMethod = !isStatic(target.getModifiers());
		Type.returnType(target).toSupertype(returns); // make sure types are compatible
		if (owner != null
			&& !raw(owner.getClass()).isAssignableTo(raw(target.getDeclaringClass()))) {
			throw new IllegalArgumentException(
					"Owner of type " + owner.getClass()
						+ " does not declare the target method: " + target);
		}
		this.hasTypeVariables = target.getTypeParameters().length > 0;
	}

	@Override
	public Type<T> type() {
		return returns;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> Produces<E> typed(Type<E> supertype) {
		type().castTo(supertype); // make sure is valid
		return (Produces<E>) this;
	}

	public boolean requestsActualType() {
		if (target.getParameterCount() == 0)
			return false;
		Type<?> parameterType = parameterType(target.getParameters()[0]);
		if (parameterType.rawType != Type.class)
			return false;
		return parameterType.parameter(0).equalTo(returns);
	}
}
