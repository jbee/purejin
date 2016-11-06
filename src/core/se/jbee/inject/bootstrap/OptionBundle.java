/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import se.jbee.inject.bootstrap.Bootstrapper.OptionBootstrapper;

/**
 * A {@link Bundle} that does different installation for different options.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 */
@FunctionalInterface
public interface OptionBundle<O> {

	/**
	 * @param bootstrapper
	 *            the {@link OptionBootstrapper} this bundle should install itself in.
	 */
	void bootstrap( OptionBootstrapper<O> bootstrapper );
}
