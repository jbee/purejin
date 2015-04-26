/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.procedure;

/**
 * The low level representation of a procedure (or operation; microservice).
 * 
 * @param <I>
 *            The type of the input
 * @param <O>
 *            The type of the output
 */
public interface Procedure<I, O> {

	O run( I input ) throws ProcedureMalfunction;

}
