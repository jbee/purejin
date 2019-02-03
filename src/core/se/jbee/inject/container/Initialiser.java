package se.jbee.inject.container;

import se.jbee.inject.Injector;

/**
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
	 * Is called by the {@link Injector} as soon as the context itself is
	 * Initialised and ready to be used by the implementation of this interface.
	 * 
	 * @param context
	 *            use to receive instances that require further initialisation
	 *            setup
	 */
	void init(T context);
}
