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
 * @param <T> The type of the instance being resolved
 * @author Jan Bernitt (jan@jbee.se)
 */
@FunctionalInterface
public interface Supplier<T> {

	/**
	 * This {@link Supplier} is asked to supply the instance that is used for
	 * the given {@link Dependency} (probably with help of the {@link
	 * Injector}).
	 */
	T supply(Dependency<? super T> dep, Injector context)
			throws UnresolvableDependency;

	/**
	 * Mostly defined to capture the contract by convention that when a {@link
	 * Supplier} class does implement {@link Generator} they are directly used
	 * as such and no generator is wrapped around the {@link Supplier} by the
	 * container.
	 *
	 * @return true, if this {@link Supplier} does support the {@link
	 * #asGenerator()} method and wants it to be used.
	 */
	default boolean isGenerator() {
		return this instanceof Generator;
	}

	/**
	 * @return This {@link Supplier} as {@link Generator}
	 *
	 * @see #isGenerator()
	 */
	default Generator<T> asGenerator() {
		return (Generator<T>) this;
	}

}
