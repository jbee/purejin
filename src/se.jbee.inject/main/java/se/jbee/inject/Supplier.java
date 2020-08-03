/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * A {@link Supplier} is a source or factory for specific instances within the
 * given {@link Injector} context.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <T> The type of the instance being resolved
 */
@FunctionalInterface
public interface Supplier<T> {

	/**
	 * This {@link Supplier} is asked to supply the instance that is used for
	 * the given {@link Dependency} (probably with help of the
	 * {@link Injector}).
	 */
	T supply(Dependency<? super T> dep, Injector context)
			throws UnresolvableDependency;

}
