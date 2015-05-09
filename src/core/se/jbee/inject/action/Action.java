/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.action;

/**
 * The low level representation of an action (a operation or micro-service).
 * 
 * @param <I>
 *            The type of the input
 * @param <O>
 *            The type of the output
 */
public interface Action<I, O> {

	O exec( I input ) throws ActionMalfunction;

}
