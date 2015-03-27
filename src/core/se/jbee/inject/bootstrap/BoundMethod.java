/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import se.jbee.inject.Parameter;
import se.jbee.inject.Type;
import se.jbee.inject.Typed;
import se.jbee.inject.util.Metaclass;

/**
 * A {@link BoundMethod} is a method bound to a particular instance 
 * (if not static) that yields/produces instances to inject.  
 *  
 * @param <T> type of the value yield by the factory method
 */
public final class BoundMethod<T>
		implements Typed<T> {

	public static <T> BoundMethod<T> bind( Object instance, Method factory, Type<T> returnType, Parameter<?>... parameters ) {
		return new BoundMethod<T>( instance, factory, returnType, parameters );
	}

	public final Object instance;
	public final Method factory;
	public final Type<T> returnType;
	public final Parameter<?>[] parameters;
	private final boolean instanceMethod;

	private BoundMethod( Object instance, Method factory, Type<T> returnType, Parameter<?>[] parameters ) {
		super();
		this.returnType = returnType;
		this.factory = factory;
		this.parameters = parameters;
		this.instance = instance;
		this.instanceMethod = !Modifier.isStatic( factory.getModifiers() );
		final Type<?> actualReturnType = Type.returnType( factory );
		if ( !actualReturnType.isAssignableTo( returnType ) ) {
			throw new IllegalArgumentException( "The producer methods methods return type `"
					+ actualReturnType + "` is not assignable to: " + returnType );
		}
		if ( instance != null && factory.getDeclaringClass() != instance.getClass() ) {
			throw new IllegalArgumentException(
					"The producer method and the instance it is invoked on have to be the same class." );
		}
		Metaclass.accessible( factory );
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
	public <E> BoundMethod<E> typed( Type<E> supertype ) {
		getType().castTo( supertype ); // make sure is valid
		return (BoundMethod<E>) this;
	}
}
