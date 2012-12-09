/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import se.jbee.inject.Name;
import se.jbee.inject.Type;

/**
 * A {@link ConstructionStrategy} picks the {@link Constructor} to use to construct objects of a
 * given {@link Class}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public interface ConstructionStrategy {

	/**
	 * @return The {@link Constructor} considered to be the reasonable or right way to construct a
	 *         object of the given type. In case one with parameters is returned the process will
	 *         try to resolve them.
	 */
	<T> Constructor<T> constructorFor( Class<T> type );

	/**
	 * @param name
	 *            Can be used as <i>hint</i> to decide between multiple existing assignable methods.
	 * @return The {@link Method} that has been chosen as implementation for the given return
	 *         {@link Type} within the given implementor {@link Class}.
	 */
	<T> Method factoryFor( Type<T> returnType, Name name, Class<?> implementor );
}
