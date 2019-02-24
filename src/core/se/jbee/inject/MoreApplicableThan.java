/*
 *  Copyright (c) 2012-2017, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * When determining what {@link Resource} is used to inject a {@link Dependency}
 * everything is sorted by applicability. The most applicable match is injected.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <T> The type of objects that are compared
 */
@FunctionalInterface
public interface MoreApplicableThan<T extends MoreApplicableThan<T>> {

	/**
	 * @return Whether or not this object or more applicable than the given one.
	 *         Equal objects are not more applicable! Also objects that have no
	 *         common context or relationship are never more applicable. For
	 *         example two {@link Type}s with no common super-type do not define
	 *         one of them as more applicable.
	 */
	boolean moreApplicableThan(T other);

}
