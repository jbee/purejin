/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.declare;

import se.jbee.inject.Env;
import se.jbee.inject.declare.Bootstrapper.Toggler;

/**
 * A {@link Bundle} that does installs {@link Bundle}s in connected to a feature
 * flag represented by an {@link Enum} constant. If the flag is
 * {@link Env#toggled(Class, Enum, Package)} the {@link Bundle} is installed,
 * otherwise it isn't.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
@FunctionalInterface
public interface Toggled<F> {

	//TODO look into doing this as some sort of Bundle extension that is transparent for the Bootstrapper
	// that also uses the environment to see if the bundle should be installed
	// => access to Environment is needed either by having Bootstrapper give it or pass it to Bundle method
	// This should also mean that Edition could be implemented "on top"

	/**
	 * @param bootstrapper the {@link Toggler} this bundle should
	 *            install itself in.
	 */
	void bootstrap(Toggler<F> bootstrapper);
}
