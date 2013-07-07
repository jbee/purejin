/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * Knows how to resolve a specific instance for the given {@link Demand}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public interface Injectable<T> {

	/**
	 * @return The instance of the given {@link Demand}.
	 */
	T instanceFor( Demand<T> demand );
}
