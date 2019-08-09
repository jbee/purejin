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
 * Users can bind an implementation for this interface. The {@link Injector}
 * will resolve all of them and call their {@link #init(Object, Injector)}
 * method as soon as the context is done.
 * 
 * This gives users the possibility to run initialisation code once and build
 * more powerful mechanisms on top of it.
 * 
 * @since 19.1
 */
@FunctionalInterface
public interface Initialiser<T> {

	/**
	 * Is called when the target instance is created within the {@link Injector}
	 * context. For {@link Injector} itself as target this is called as soon as
	 * its state has been initialised. It can be used to decorate the injector
	 * or other target instances.
	 * 
	 * @param target the newly created instance to initialise
	 * @param context use to receive instances that require further
	 *            initialisation setup
	 * @return the initialised instance; usually this is still the target
	 *         instance. When using proxy or decorator pattern this would be the
	 *         proxy or decorator instance.
	 */
	T init(T target, Injector context);

	// * If T is returned this could be used to do decorations (but the init would have to take place so that the decorated instance ends up in scope => looks like it already is)
	// * For annotations bind a Initialiser<Object> with a hierarchy injectingInto(MyAnnotation.class)
	// * When returning T this could even be used to wrap the Injector itself and that way extend it which could be used to make annotations work
}
