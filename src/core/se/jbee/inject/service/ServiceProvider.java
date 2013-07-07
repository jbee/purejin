/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.service;

import se.jbee.inject.Type;

/**
 * A {@link ServiceProvider} resolves {@link ServiceMethod}s by {@link Type}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public interface ServiceProvider {

	/**
	 * @return the {@link ServiceMethod} that implements a service having the given parameter and
	 *         return {@link Type}.
	 */
	<P, R> ServiceMethod<P, R> provide( Type<P> parameterType, Type<R> returnType );
}
