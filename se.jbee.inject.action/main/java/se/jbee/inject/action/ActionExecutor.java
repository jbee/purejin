/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.action;

import se.jbee.inject.UnresolvableDependency.SupplyFailed;

import java.lang.reflect.Method;

/**
 * The {@link ActionExecutor} invokes the actual action {@link Method}. It is an
 * abstraction for the inner mechanics of {@link Action}s so that these can be
 * customised by replacing the {@link ActionExecutor} by making a bind (see
 * {@link ActionModule}).
 *
 * @see Action
 */
@FunctionalInterface
public interface ActionExecutor {

	/**
	 * Runs an {@link Action} by invoking the underlying method.
	 *
	 * @param args all resolved arguments for the method (in order)
	 * @param value provided (also one of the arguments)
	 * @throws ActionExecutionFailed in case of any {@link Exception} during
	 *             execution. The cause should be the exception causing the
	 *             problem, not another wrapper like {@link SupplyFailed}.
	 */
	<A, B> B execute(ActionSite<A, B> site, Object[] args, A value)
			throws ActionExecutionFailed;
}
