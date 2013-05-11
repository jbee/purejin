/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * <i>Has a {@link Resource}</i>
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public interface Resourced<T> {

	/**
	 * @return The {@link Resource} of this compound.
	 */
	Resource<T> getResource();
}
