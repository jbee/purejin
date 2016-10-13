/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

import se.jbee.inject.bootstrap.Bundle;
import se.jbee.inject.bootstrap.Module;

/**
 * An {@link Edition} decides wich features are contained in a specific setup.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
@FunctionalInterface
public interface Edition {

	/**
	 * Default {@link Edition} that has all features.
	 */
	Edition FULL = new Edition() {

		@Override
		public boolean featured( Class<?> bundleOrModule ) {
			return true;
		}
	};

	/**
	 * @return true if the given {@link Class} of a {@link Module} or {@link Bundle} should be
	 *         included in the context created (should be installed).
	 */
	boolean featured( Class<?> bundleOrModule );

}
