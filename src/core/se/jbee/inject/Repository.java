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
	 * @param serialID ID number of this {@link InjectionCase} with the {@link Injector}
	 *            context
	 * @param dep currently served {@link Dependency}
	 * @param provider constructor function yielding new instances if needed
	 * @return Existing instances are returned, non-existing are received from the
	 *         given {@link Provider} and added to this {@link Repository} (forever
	 *         if it is an application wide singleton or shorter depending on the
	 *         {@link Scope} that created this {@link Repository}).
	 * 
	 *         The information from the {@link Dependency} and {@link InjectionCase} can
	 *         be used to lookup existing instances.
	 */
	<T> T serve(int serialID, Dependency<? super T> dep, Provider<T> provider) throws UnresolvableDependency;
}
