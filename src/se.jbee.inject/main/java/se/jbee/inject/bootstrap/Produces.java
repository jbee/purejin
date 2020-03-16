/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Utils.accessible;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import se.jbee.inject.Parameter;
import se.jbee.inject.Type;
import se.jbee.inject.Typed;
import se.jbee.inject.declare.ValueBinder;

/**
 * A {@link Produces} is the {@link ValueBinder} expansion wrapper for a method bound
 * to a particular instance (if not static) that produces instances to inject.
 *
 * @param <T> type of the value yield by the factory method
 */
public final class Produces<T> implements Typed<T> {

	public static Produces<?> bind(Object owner, Method target,
			Parameter<?>... hints) {
		return new Produces<>(owner, target, hints);
	}

	public final Object owner;
	public final Method target;
	public final Type<T> returns;
	public final Parameter<?>[] hints;
	public final boolean isInstanceMethod;

	@SuppressWarnings("unchecked")
	private Produces(Object owner, Method target, Parameter<?>[] hints) {
		this.returns = (Type<T>) Type.returnType(target);
		this.target = accessible(target);
		this.hints = hints;
		this.owner = owner;
		this.isInstanceMethod = !Modifier.isStatic(target.getModifiers());
		Type.returnType(target).toSupertype(returns); // make sure types are compatible
		if (owner != null
			&& !owner.getClass().isAssignableFrom(target.getDeclaringClass())) {
			throw new IllegalArgumentException(
					"The target method and the owner it is invoked on have to be the same class.");
		}
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
}
