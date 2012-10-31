/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package de.jbee.inject;

public interface Scope {

	/**
	 * Creates a empty {@link Repository} that stores instances in this {@link Scope}.
	 * 
	 * @return a empty instance in this {@linkplain Scope}.
	 */
	Repository init();
}
