/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * When determining what {@link Resource} is used to inject a {@link Dependency} everything is
 * sorted by its {@link Precision}. The most precise matching will be used to inject.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <T>
 *            The type of objects that are compared
 */
public interface PreciserThan<T extends PreciserThan<T>> {

	/**
	 * @return Whether or not this object or more precise than the given one. Equal objects are not
	 *         more precise! Also objects that have no common context or relationship are never more
	 *         precise. An example would be that two {@link Type}s with no common super-type do not
	 *         define one of them that is more precise.
	 */
	boolean morePreciseThan( T other );
}
