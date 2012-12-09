/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

/**
 * An {@link Edition} decides wich features are contained in a specific setup.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
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
