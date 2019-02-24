/*
 *  Copyright (c) 2012-2017, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * A {@link Generator} creates the instance(s) for the generators
 * {@link InjectionCase}.
 * 
 * @since 19.1
 */
@FunctionalInterface
public interface Generator<T> {

	T instanceFor(Dependency<? super T> dependency)
			throws UnresolvableDependency;

}
