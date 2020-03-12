/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import se.jbee.inject.bootstrap.Bootstrapper.ToggledBootstrapper;
import se.jbee.inject.config.Env;
import se.jbee.inject.declare.Bundle;

/**
 * A {@link Bundle} that does installs {@link Bundle}s in connected to a feature
 * flag represented by an {@link Enum} constant. If the flag is
 * {@link Env#toggled(Class, Enum, Package)} the {@link Bundle} is installed,
 * otherwise it isn't.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
@FunctionalInterface
public interface ToggledBundles<C> {

	//TODO look into doing this as some sort of Bundle extension that is transparent for the Bootstrapper
	// that also uses the environment to see if the bundle should be installed
	// => access to Environment is needed either by having Bootstrapper give it or pass it to Bundle method
	// This should also mean that Edition could be implemented "on top"

	/**
	 * @param bootstrapper the {@link ToggledBootstrapper} this bundle should
	 *            install itself in.
	 */
	void bootstrap(ToggledBootstrapper<C> bootstrapper);
}
