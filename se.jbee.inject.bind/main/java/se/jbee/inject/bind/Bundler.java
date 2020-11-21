/*
 *  Copyright (c) 2012-2019, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

/**
 * Determines all reachable {@link Bundle}s starting from a root {@link Bundle}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
@FunctionalInterface
public interface Bundler {

	/**
	 * @param root origin of reachable computation
	 * @return All {@link Bundle}s (their {@link Class}es) that are reachable
	 *         (installed) when starting from the given root {@link Bundle}.
	 */
	Class<? extends Bundle>[] bundle(Class<? extends Bundle> root);
}
