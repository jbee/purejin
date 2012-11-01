/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * Manages the already created instances.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 */
public interface Repository {

	/**
	 * @return Existing instances are returned, non-existing are received from the given
	 *         {@link Injectable} and stocked in the scope of this {@link Repository} (forever if it
	 *         is an application wide singleton).
	 */
	<T> T serve( Demand<T> demand, Injectable<T> injectable );
}
