/*
 *  Copyright (c) 2012-2017, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * Manages the already created instances but does not know how to create them.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
@FunctionalInterface
public interface Repository {

	/**
	 * @param dep currently served {@link Dependency}
	 * @param spec       the {@link Specification} of the {@link Generator}
	 *                   providing new instances if needed
	 * @param provider   constructor function yielding new instances if needed
	 * @return Existing instances are returned, non-existing are received from the
	 *         given {@link Provider} and added to this {@link Repository} (forever
	 *         if it is an application wide singleton or shorter depending on the
	 *         {@link Scope} that created this {@link Repository}).
	 * 
	 *         The information from the {@link Dependency} and {@link Specification}
	 *         can be used to lookup existing instances.
	 */
	<T> T serve(int serialID, Dependency<? super T> dep, Provider<T> provider) throws UnresolvableDependency;
}
