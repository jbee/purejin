/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * A {@link Generator} creates the instance(s) for the generator's
 * {@link InjectionCase}.
 * 
 * When binding directly to a {@link Generator} any {@link Scoping} will be
 * ineffective since the supplied {@link Generator} will directly be asked to
 * {@link #yield(Dependency)} the instance for the dependency. This can be used
 * to implement instance management different to the one provided by
 * {@link Scope}s or to simply avoid unnecessary indirection or processing.
 * 
 * @since 19.1
 */
@FunctionalInterface
public interface Generator<T> {

	/**
	 * Yields the instance that satisfies the given {@link Dependency}.
	 * 
	 * @param dep describes the requested instance and injection situation
	 * @return the instance to use for the given {@link Dependency}
	 * @throws UnresolvableDependency in case this {@link Generator} is unable
	 *             to create the requested instance because another instance
	 *             needed to create it could not be resolved.
	 */
	T yield(Dependency<? super T> dep) throws UnresolvableDependency;

}
