/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import se.jbee.lang.Type;

import static se.jbee.lang.Type.raw;

/**
 * A {@link Generator} creates the instance(s) for the generator's
 * {@link Resource}.
 *
 * When binding directly to a {@link Generator} any {@link ScopeLifeCycle} will be
 * ineffective since the supplied {@link Generator} will directly be asked to
 * {@link #generate(Dependency)} the instance for the dependency. This can be used
 * to implement instance management different to the one provided by
 * {@link Scope}s or to simply avoid unnecessary indirection or processing.
 *
 * @since 8.1
 */
@FunctionalInterface
public interface Generator<T> {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <T> Type<Generator<T>> generatorTypeOf(Type<T> providedType) {
		return (Type) raw(Generator.class).parameterized(providedType);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <T> Type<Generator<T>[]> generatorsTypeOf(Type<T> generatedType) {
		return (Type) raw(Generator[].class).parameterized(generatedType);
	}

	/**
	 * Yields the instance that satisfies the given {@link Dependency}.
	 *
	 * @param dep describes the requested instance and injection situation
	 * @return the instance to use for the given {@link Dependency}
	 * @throws UnresolvableDependency in case this {@link Generator} is unable
	 *             to create the requested instance because another instance
	 *             needed to create it could not be resolved.
	 */
	T generate(Dependency<? super T> dep) throws UnresolvableDependency;

}
