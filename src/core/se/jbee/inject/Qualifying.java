/*
 *  Copyright (c) 2012-2019, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * When determining what {@link Locator} is used to inject a {@link Dependency}
 * everything is sorted starting with the most {@link Qualifying}. The most
 * qualified match is injected.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <T> The type of objects that are compared
 */
@FunctionalInterface
public interface Qualifying<T extends Qualifying<T>> {

	/**
	 * @return Whether or not this object or more qualified than the given one.
	 * 
	 *         Equal objects are do not define one of them as more qualified!
	 *         Also objects that have no common context or relationship don't
	 *         define one of them as more qualified.
	 * 
	 *         For example two {@link Type}s with no common super-type do not
	 *         define one of them as more qualified.
	 */
	boolean moreQualiedThan(T other);

	static <A extends Qualifying<? super A>, B extends Qualifying<? super B>> boolean compareRelated(
			A a1, A a2, B b1, B b2) {
		return a1.moreQualiedThan(a2) // sequence in OR is very important!!!
			|| !a2.moreQualiedThan(a1) && b1.moreQualiedThan(b2);
	}

	static <A extends Qualifying<? super A>> int compare(A one, A other) {
		return one.moreQualiedThan(other)
			? -1
			: other.moreQualiedThan(one) ? 1 : 0;
	}

}
