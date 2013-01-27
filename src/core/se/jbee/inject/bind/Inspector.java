/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import se.jbee.inject.Name;
import se.jbee.inject.Parameter;

/**
 * A strategy to extract missing information from types that is used within the {@link Binder} to
 * allow semi-automatic bindings.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public interface Inspector {

	/**
	 * Picks the {@link Constructor} to use to construct objects of a given {@link Class}.
	 * 
	 * @return The {@link Constructor} considered to be the reasonable or right way to construct a
	 *         object of the given type. In case one with parameters is returned the process will
	 *         try to resolve them.
	 */
	<T> Constructor<T> constructorFor( Class<T> type );

	/**
	 * @return The {@link Member}s that should be bound from the given implementor.
	 */
	<T> Method[] methodsIn( Class<T> implementor );

	Name nameFor( AccessibleObject obj );

	Parameter<?>[] parametersFor( AccessibleObject obj );
}
