/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import se.jbee.inject.Parameter;
import se.jbee.inject.Type;
import se.jbee.inject.Typed;

public final class Producible<T>
		implements Typed<T> {

	public static Producible<?> producible( Method producer, Parameter<?>[] parameters,
			Object instance ) {
		return producible( Type.returnType( producer ), producer, parameters, instance );
	}

	public static <T> Producible<T> producible( Type<T> returnType, Method producer,
			Parameter<?>[] parameters, Object instance ) {
		return new Producible<T>( returnType, producer, parameters, instance );
	}

	public final Type<T> returnType;
	public final Method producer;
	public final Parameter<?>[] parameters;
	public final Object instance;
	private final boolean instanceMethod;

	private Producible( Type<T> returnType, Method producer, Parameter<?>[] parameters,
			Object instance ) {
		super();
		this.returnType = returnType;
		this.producer = producer;
		this.parameters = parameters;
		this.instance = instance;
		this.instanceMethod = !Modifier.isStatic( producer.getModifiers() );
		final Type<?> actualReturnType = Type.returnType( producer );
		if ( !actualReturnType.isAssignableTo( returnType ) ) {
			throw new IllegalArgumentException( "The producer methods methods return type `"
					+ actualReturnType + "` is not assignable to: " + returnType );
		}
		if ( instance != null && producer.getDeclaringClass() != instance.getClass() ) {
			throw new IllegalArgumentException(
					"The producer method and the instance it is invoked on have to be the same class." );
		}
		Metaclass.accessible( producer );
	}

	public boolean isInstanceMethod() {
		return instanceMethod;
	}

	@Override
	public Type<T> getType() {
		return returnType;
	}

	@SuppressWarnings ( "unchecked" )
	@Override
	public <E> Producible<E> typed( Type<E> supertype ) {
		getType().castTo( supertype );
		return (Producible<E>) this;
	}
}
