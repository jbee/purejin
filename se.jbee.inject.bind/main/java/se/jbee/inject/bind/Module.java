/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.Env;

/**
 * {@link Bindings} are defined with {@link Module}s while {@link Bundle}s are
 * used to group multiple {@link Module}s and {@link Bundle}s what allows to
 * build graphs of {@link Bundle}s with {@link Module}s as leaves.
 *
 * @see Bundle
 * @see ModuleWith
 */
@FunctionalInterface
public interface Module {

	/**
	 * @param bindings use to declare made bound within this {@link Module}.
	 * @param env the {@link Env} used during the bootstrapping of the
	 *            {@link Module}
	 */
	void declare(Bindings bindings, Env env);

}
