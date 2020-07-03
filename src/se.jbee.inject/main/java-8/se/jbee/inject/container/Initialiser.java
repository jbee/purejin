/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.container;

import se.jbee.inject.Injector;

/**
 * An {@link Initialiser} is like an interceptor that is called after an object
 * is created.
 * 
 * {@link Initialiser}s are matched based on the actual type of the target
 * argument. If target can be assigned to the {@link Initialiser}'s target type
 * the {@link Initialiser#init(Object, Injector)} is called.
 * 
 * {@link Initialiser}s allow to run initialisation code once and build more
 * powerful mechanisms on top of it.
 * 
 * @since 19.1
 */
@FunctionalInterface
public interface Initialiser<T> {

	/**
	 * Is called when the target instance is created within the {@link Injector}
	 * context. For {@link Injector} itself as target this is called as soon as
	 * its state has been initialised. It can be used to decorate the
	 * {@link Injector} or any other target instance created within a context.
	 * 
	 * @param target the newly created instance to initialise. For
	 *            {@link Initialiser} of the {@link Injector} this is always the
	 *            decorated {@link Injector} in case decoration was done by
	 *            other {@link Initialiser}s.
	 * @param context use to resolve instances that require further
	 *            initialisation setup. For {@link Initialiser} of the
	 *            {@link Injector} this is always the underlying
	 *            {@link Injector} that isn't decorated in case decoration was
	 *            done before by another {@link Initialiser}.
	 * @return the initialised instance; usually this is still the target
	 *         instance. When using proxy or decorator pattern this would be the
	 *         proxy or decorator instance.
	 */
	T init(T target, Injector context);

}
