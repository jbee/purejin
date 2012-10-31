/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

public interface PreciserThan<T extends PreciserThan<T>> {

	boolean morePreciseThan( T other );
}
