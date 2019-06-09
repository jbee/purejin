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
import se.jbee.inject.Utils;

/**
 * A {@link BoundMethod} is a method bound to a particular instance (if not
 * static) that yields/produces instances to inject.
 *
 * @param <T> type of the value yield by the factory method
 */
public final class BoundMethod<T> implements Typed<T> {

	public static <T> BoundMethod<T> bind(Object instance, Method producer,
			Type<T> returnType, Parameter<?>... parameters) {
		return new BoundMethod<>(instance, producer, returnType, parameters);
	}

	public final Object instance;
	public final Method producer;
	public final Type<T> returnType;
	public final Parameter<?>[] parameters;
	public final boolean isInstanceMethod;

	private BoundMethod(Object instance, Method producer, Type<T> returnType,
			Parameter<?>[] parameters) {
		this.returnType = returnType;
		this.producer = accessible(producer);
		this.parameters = parameters;
		this.instance = instance;
		this.isInstanceMethod = !Modifier.isStatic(producer.getModifiers());
		final Type<?> actualReturnType = Type.returnType(producer);
		actualReturnType.toSupertype(returnType); // make sure types are compatible
		if (instance != null
			&& producer.getDeclaringClass() != instance.getClass()) {
			throw new IllegalArgumentException(
					"The producer method and the instance it is invoked on have to be the same class.");
		}
		Utils.accessible(producer);
	}

	@Override
	public Type<T> type() {
		return returnType;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> BoundMethod<E> typed(Type<E> supertype) {
		type().castTo(supertype); // make sure is valid
		return (BoundMethod<E>) this;
	}
}
