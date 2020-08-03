/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.binder;

import static se.jbee.inject.Utils.accessible;

import java.lang.reflect.Constructor;

import se.jbee.inject.Parameter;
import se.jbee.inject.Type;
import se.jbee.inject.Typed;
import se.jbee.inject.bind.ValueBinder;

/**
 * A {@link New} is the {@link ValueBinder} expansion wrapper for {@link Constructor}
 * usages or the container equivalent of a {@code new} statement.
 * 
 * @param <T> Type of object created
 */
public final class New<T> implements Typed<T> {

	public static <T> New<T> bind(Constructor<T> target,
			Parameter<?>... params) {
		return new New<>(target, params);
	}

	public final Constructor<T> target;
	public final Parameter<?>[] hints;

	private New(Constructor<T> target, Parameter<?>[] hints) {
		this.target = accessible(target);
		this.hints = hints;
	}

	@Override
	public Type<T> type() {
		return Type.raw(target.getDeclaringClass());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> New<E> typed(Type<E> supertype) {
		type().castTo(supertype);
		return (New<E>) this;
	}

}
