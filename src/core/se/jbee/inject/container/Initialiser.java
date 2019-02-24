package se.jbee.inject.container;

import se.jbee.inject.Injector;

/**
 * An {@link Initialiser} is like an interceptor that is called after an object
 * is created.
 * 
 * Users can bind an implementation for this interface. The {@link Injector}
 * will resolve all of them and call their {@link #init(Injector)} method as
 * soon as the context is done.
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
	 * context. For {@link Injector} itself as target this is called as soon as its
	 * state has been initialised.
	 * 
	 * @param target  the newly created instance to initialise
	 * @param context use to receive instances that require further initialisation
	 *                setup
	 */
	void init(T target, Injector context);
}
