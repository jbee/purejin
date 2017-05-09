/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.action;

/**
 * The low user level representation of an action (a operation or micro-service).
 * 
 * @param <I>
 *            The type of the input
 * @param <O>
 *            The type of the output
 */
@FunctionalInterface
public interface Action<I, O> {

	/**
	 * Runs the action to compute the output from the input. This typically does
	 * not change any inner state but it might in rare cases be part of an
	 * action to do such a thing.
	 * 
	 * @param input
	 *            might be null for {@link Void} arguments or when argument was
	 *            resolved to null
	 * @return might be null
	 * @throws ActionMalfunction
	 *             wraps all {@link RuntimeException}s
	 */
	O exec( I input ) throws ActionMalfunction;

}
