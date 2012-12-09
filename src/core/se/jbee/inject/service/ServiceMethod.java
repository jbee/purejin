/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.service;

import se.jbee.inject.bind.Extension;

/**
 * The low level representation of a service.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <R>
 *            The return type of the method wired to this service
 * @param <P>
 *            The type of the parameter of the service
 */
public interface ServiceMethod<P, R> {

	/**
	 * @return the value results from the execution of this service with the given argument as
	 *         parameter.
	 */
	R invoke( P params );

	enum ServiceClassExtension
			implements Extension<ServiceClassExtension, Object> {
		// no different options
	}
}
