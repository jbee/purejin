/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package de.jbee.inject;

/**
 * Knows how to resolve a specific instance for the given {@link Demand}.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Injectable<T> {

	T instanceFor( Demand<T> demand );
}
