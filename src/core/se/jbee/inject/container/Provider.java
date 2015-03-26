/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.container;

import se.jbee.inject.Supplier;
import se.jbee.inject.bootstrap.Bundle;

/**
 * A indirection that resolves the instance lazily when {@link #provide()} is invoked. This is
 * mainly used to allow the injection and usage of instances that have a more unstable scope into an
 * instance of a more stable scope.
 * 
 * In contrast to other DI-frameworks this is no core concept. To enable {@linkplain Provider}s
 * install the them through buildin-{@link Bundle}.
 * 
 * But it is also very easy to use another similar provider interface by installing such a bridge
 * {@link Supplier}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public interface Provider<T> {

	/**
	 * @return the current instance.
	 */
	T provide();
}
