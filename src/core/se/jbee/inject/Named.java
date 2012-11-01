/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * Names are used to distinguish {@link Instance}s of the same {@link Type}.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Named {

	/**
	 * @return the name of an {@link Instance} / {@link Resource}.
	 */
	Name getName();
}
