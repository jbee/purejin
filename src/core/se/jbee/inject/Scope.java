/*
 *  Copyright (c) 2012-2017, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * A {@linkplain Scope} describes a particular lifecycle.
 * 
 * Thereby the {@linkplain Scope} itself acts as a factory for
 * {@link Repository}s. Each {@link Injector} has a single
 * {@linkplain Repository} for each {@linkplain Scope}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
@FunctionalInterface
public interface Scope {

	/**
	 * Creates a empty {@link Repository} that stores instances in this
	 * {@link Scope}.
	 * 
	 * @param generators the total number of {@link Generator}s in the
	 *            {@link Injector} context
	 * 
	 * @return a empty instance in this {@linkplain Scope}.
	 */
	Repository init(int generators);

	/**
	 * Should be implemented by all {@link Scope}s that produce stable
	 * "singleton" instances. That means for a particular {@link Dependency} the
	 * {@link Scope}'s {@link Repository} will always yield the same instance
	 * {@link Object} which does not become out-dated at some point like
	 * something scoped per session, request or thread.
	 * 
	 * @since 19.1
	 */
	interface SingletonScope extends Scope {
	}
}
