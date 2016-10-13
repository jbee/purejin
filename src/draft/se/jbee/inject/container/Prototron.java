/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.container;

import se.jbee.inject.Parameter;

/**
 * Unifying interface for a "prepared" constructor and method invocations.
 * 
 * Prototype + Singleton => Prototron 
 * 
 * By default required arguments are resolved by type from the container. When
 * {@link Parameter}s are provided the matching parameter uses the provided
 * argument resolution instead.
 */
public interface Prototron<T> {

	Prototron<T> bind(Parameter<?>...parameters);
	
	T newInstance();
}
