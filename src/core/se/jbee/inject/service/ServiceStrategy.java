/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.service;

import java.lang.reflect.Method;

/**
 * The {@link ServiceStrategy} picks the {@link Method}s that are used to implement
 * {@link ServiceMethod}s. This abstraction allows to customize what methods are bound as
 * {@link ServiceMethod}s.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public interface ServiceStrategy {

	/**
	 * All methods in the given {@link Class} that should be used to implement a
	 * {@link ServiceMethod}.
	 */
	Method[] serviceMethodsIn( Class<?> serviceClass );
}
