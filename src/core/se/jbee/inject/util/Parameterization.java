/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import se.jbee.inject.Supplier;
import se.jbee.inject.Type;
import se.jbee.inject.Typed;

/**
 * A {@link Parameterization} is a {@link Supplier} for parameters of {@link Constructor} or
 * {@link Method} invocations.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <T>
 *            The {@link Type} of the parameter
 */
public interface Parameterization<T>
		extends Typed<T>, Supplier<T> {

	/**
	 * @param type
	 *            The new type of this {@link Parameterization}
	 * @throws UnsupportedOperationException
	 *             In case the given type is incompatible with the previous one.
	 */
	@Override
	<E> Parameterization<E> typed( Type<E> type )
			throws UnsupportedOperationException;
}
