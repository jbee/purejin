/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package de.jbee.inject.service;

import java.lang.reflect.Method;

public interface ServiceStrategy {

	/**
	 * Allows to customize what methods are bound as {@link ServiceMethod}s.
	 */
	Method[] serviceMethodsIn( Class<?> serviceClass );
}
