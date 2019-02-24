/*
 *  Copyright (c) 2012-2017, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import se.jbee.inject.Name;
import se.jbee.inject.Parameter;
import se.jbee.inject.bind.Binder;

/**
 * A strategy to extract missing information from types that is used within the
 * {@link Binder} to allow semi-automatic bindings.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public interface Inspector {

	/**
	 * Picks the {@link Constructor} to use to construct objects of a given
	 * {@link Class}.
	 * 
	 * @return The {@link Constructor} considered to be the reasonable or right
	 *         way to construct a object of the given type. In case one with
	 *         parameters is returned the process will try to resolve them.
	 */
	<T> Constructor<T> constructorFor(Class<T> type);

	/**
	 * @return The {@link Member}s that should be bound from the given
	 *         implementor.
	 */
	<T> Method[] methodsIn(Class<T> implementor);

	/**
	 * @return The {@link Name} of the instance provided by the given object.
	 *         Use {@link Name#DEFAULT} for no specific name.
	 */
	Name nameFor(AccessibleObject obj);

	/**
	 * @return The {@link Parameter} hints for the construction/invocation of
	 *         the given object. Use a zero length array if there are no hits.
	 */
	Parameter<?>[] parametersFor(AccessibleObject obj);
}
