/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import se.jbee.lang.Type;

import static se.jbee.lang.Type.raw;

/**
 * A indirection that resolves the instance lazily when {@link #provide()} is
 * invoked. This is mainly used to allow the injection and usage of instances
 * that have a more unstable scope into an instance of a more stable scope.
 *
 * Usage of {@link Provider}s to circumvent scoping limitations is explicitly
 * installed using the {@code CoreFeature#PROVIDER}.
 *
 * But it is also very easy to use another similar provider interface by
 * installing a similar bridge {@link Supplier}.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
@FunctionalInterface
public interface Provider<T> {

	static <T> Type<Provider<T>> providerTypeOf(Class<T> providedType) {
		return providerTypeOf(raw(providedType));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <T> Type<Provider<T>> providerTypeOf(Type<T> providedType) {
		return (Type) raw(Provider.class).parameterized(providedType);
	}

	/**
	 * @return The lazily resolved instance. Implementations should make sure
	 *         that calling this method multiple times does cache the result if
	 *         that is semantically correct.
	 * @throws UnresolvableDependency in case the underlying implementation
	 *             could not provide the instance due to lack of suitable
	 *             declarations.
	 */
	T provide() throws UnresolvableDependency;
}
