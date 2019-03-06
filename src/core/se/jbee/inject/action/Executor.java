/*
 *  Copyright (c) 2012-2019, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.action;

import java.lang.reflect.Method;

import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency.SupplyFailed;

/**
 * The {@link Executor} invokes the actual action {@link Method}. It is an
 * abstraction for the inner mechanics of {@link Action}s so that these can be
 * customised by replacing the {@link Executor} by making a bind (see
 * {@link ActionModule}).
 * 
 * @see Action
 */
@FunctionalInterface
public interface Executor {

	/**
	 * Runs an {@link Action} by invoking the underlying method.
	 * 
	 * @param impl owner of the action method
	 * @param action method to run
	 * @param args all resolved arguments for the method (in order)
	 * @param output type of the result
	 * @param input parameter type
	 * @param value provided (also one of the arguments)
	 * @return result of the invocation
	 * @throws ActionMalfunction in case of any {@link Exception} during
	 *             execution. The cause should be the exception causing the
	 *             problem, not another wrapper like {@link SupplyFailed}.
	 */
	<I, O> O exec(Object impl, Method action, Object[] args, Type<O> output,
			Type<I> input, I value) throws ActionMalfunction;
}
