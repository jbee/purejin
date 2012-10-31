/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package de.jbee.inject.bind;

public interface Edition {

	Edition FULL = new Edition() {

		@Override
		public boolean featured( Class<?> bundleOrModule ) {
			return true;
		}
	};

	boolean featured( Class<?> bundleOrModule );
}
