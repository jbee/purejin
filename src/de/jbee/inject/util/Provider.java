package de.jbee.inject.util;

import de.jbee.inject.Supplier;

/**
 * A indirection that resolves the instance lazily when {@link #provide()} is invoked. This is
 * mainly used to allow the injection and usage of instances that have a more unstable scope into an
 * instance of a more stable scope.
 * 
 * In contrast to other DI-frameworks this is no core concept. To enable {@linkplain Provider}s
 * install the {@link SuppliedBy#PROVIDER_BRIDGE}.
 * 
 * But it is also very easy to use another similar provider interface by installing such a bridge
 * {@link Supplier}.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Provider<T> {

	T provide();
}
