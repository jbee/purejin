/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package de.jbee.inject;

/**
 * Manages the already created instances.
 * 
 * Existing instances are returned, non-existing are received from the given {@link Injectable} and
 * stocked forever.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 */
public interface Repository {

	<T> T serve( Demand<T> demand, Injectable<T> injectable );
}
