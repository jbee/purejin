/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.service;

/**
 * The low level representation of a service.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <I>
 *            The type of the parameter of the service
 * @param <O>
 *            The return type of the method wired to this service
 */
public interface ServiceMethod<I, O> {

	/**
	 * @return the value results from the execution of this service with the
	 *         given argument as parameter.
	 */
	O invoke( I params ) throws ServiceMalfunction;

}
