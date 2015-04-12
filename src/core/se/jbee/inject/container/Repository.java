/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.container;

import se.jbee.inject.Dependency;
import se.jbee.inject.InjectronInfo;
import se.jbee.inject.UnresolvableDependency;

/**
 * Manages the already created instances but does not know how to create them.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 */
public interface Repository {

	/**
	 * @return Existing instances are returned, non-existing are received from
	 *         the given {@link Provider} and added to this {@link Repository}
	 *         (forever if it is an application wide singleton or shorter
	 *         depending on the scope). {@link Dependency} and
	 *         {@link InjectronInfo} can be used to lookup existing instances.
	 */
	<T> T serve( Dependency<? super T> dependency, InjectronInfo<T> info, Provider<T> provider ) throws UnresolvableDependency;
}
