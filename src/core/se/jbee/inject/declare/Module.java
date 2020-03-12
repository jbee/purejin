/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.declare;

import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.config.Env;

/**
 * {@link Bindings} are defined with {@link Module}s while {@link Bundle}s are
 * used to group multiple {@link Module}s and {@link Bundle}s what allows to
 * build graphs of {@link Bundle}s with {@link Module}s as leafs.
 * 
 * @see Bundle
 * @see ModuleWith
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
@FunctionalInterface
public interface Module {

	/**
	 * @param bindings use to declare made bound within this {@link Module}.
	 */
	void declare(Bindings bindings, Env env);

}
