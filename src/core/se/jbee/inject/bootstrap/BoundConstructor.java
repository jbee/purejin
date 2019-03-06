/*
 *  Copyright (c) 2012-2019, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Utils.accessible;

import java.lang.reflect.Constructor;

import se.jbee.inject.Parameter;
import se.jbee.inject.Type;
import se.jbee.inject.Typed;

public final class BoundConstructor<T> implements Typed<T> {

	public static <T> BoundConstructor<T> bind(Constructor<T> constructor,
			Parameter<?>... parameters) {
		return new BoundConstructor<>(constructor, parameters);
	}

	public final Constructor<T> constructor;
	public final Parameter<?>[] parameters;

	private BoundConstructor(Constructor<T> constructor,
			Parameter<?>[] parameters) {
		this.constructor = accessible(constructor);
		this.parameters = parameters;
	}

	@Override
	public Type<T> type() {
		return Type.raw(constructor.getDeclaringClass());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> BoundConstructor<E> typed(Type<E> supertype) {
		type().castTo(supertype);
		return (BoundConstructor<E>) this;
	}

}
