/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.Name;

/**
 * A strategy that derives {@link Name}s from an object passed as argument.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <T>
 *            The type of value that can be transformed to a {@link Name}
 */
public interface Naming<T> {

	/**
	 * <b>Note:</b> Dependent on the usage it should be considered to use
	 * {@link Name#namedInternal(String)} to derive the names whenever there is a chance that the
	 * name might collide with a user defined one.
	 * 
	 * @param value
	 *            null is allowed
	 * @return A {@link Name} derived from the value passed
	 */
	Name name( T value );
}
