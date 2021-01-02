/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import se.jbee.inject.bind.Bundle;

/**
 * Determines all reachable {@link Bundle}s starting from a root {@link Bundle}.
 *
 * This is more a utility API for testing.
 */
@FunctionalInterface
public interface BundleBootstrapper {

	/**
	 * @param root origin of reachable computation
	 * @return All {@link Bundle}s (their {@link Class}es) that are reachable
	 *         (installed) when starting from the given root {@link Bundle}.
	 */
	Class<? extends Bundle>[] installedBundles(Class<? extends Bundle> root);
}
