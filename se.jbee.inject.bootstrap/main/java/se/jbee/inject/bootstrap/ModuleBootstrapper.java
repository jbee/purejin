/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.Module;

/**
 * Determines / extracts the {@link Module}s from a root {@link Bundle}.
 *
 * This is more a utility API for testing.
 */
@FunctionalInterface
public interface ModuleBootstrapper {

	/**
	 * @return All {@link Module}s that result from expanding the given root
	 *         {@link Bundle} to the module level.
	 */
	Module[] installedModules(Class<? extends Bundle> root);
}
