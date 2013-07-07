/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.util;

import java.lang.reflect.Constructor;

import se.jbee.inject.Parameter;
import se.jbee.inject.Type;
import se.jbee.inject.Typed;

public final class Constructible<T>
		implements Typed<T> {

	public static <T> Constructible<T> constructible( Constructor<T> constructor,
			Parameter<?>... parameters ) {
		return new Constructible<T>( constructor, parameters );
	}

	public final Constructor<T> constructor;
	public final Parameter<?>[] parameters;

	private Constructible( Constructor<T> constructor, Parameter<?>[] parameters ) {
		super();
		this.constructor = constructor;
		this.parameters = parameters;
	}

	@Override
	public Type<T> getType() {
		return Type.raw( constructor.getDeclaringClass() );
	}

	@SuppressWarnings ( "unchecked" )
	@Override
	public <E> Constructible<E> typed( Type<E> supertype ) {
		getType().castTo( supertype );
		return (Constructible<E>) this;
	}

}
