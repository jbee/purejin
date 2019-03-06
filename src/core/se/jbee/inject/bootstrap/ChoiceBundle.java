/*
 *  Copyright (c) 2012-2019, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import se.jbee.inject.bootstrap.Bootstrapper.ChoiceBootstrapper;
import se.jbee.inject.config.Choices;

/**
 * A {@link Bundle} that does different installation for different
 * {@link Choices}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 */
@FunctionalInterface
public interface ChoiceBundle<C> {

	/**
	 * @param bootstrapper the {@link ChoiceBootstrapper} this bundle should
	 *            install itself in.
	 */
	void bootstrap(ChoiceBootstrapper<C> bootstrapper);
}
