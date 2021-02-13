package se.jbee.inject.config;

import se.jbee.lang.Type;

import java.lang.reflect.Method;

@FunctionalInterface
public interface Connector {

	/**
	 * Callback function called by during instance instantiation if methods of
	 * the instance should be connected elsewhere, namely by the means of this
	 * {@link Connector} to the endpoint it is connecting.
	 *
	 * @param instance  the subject instance that should receive the call
	 * @param as        the type as which the instance is used
	 *                  (injected/resolved). This is either the exact {@link
	 *                  Class} type of the instance or one of its supertypes.
	 * @param connected the {@link Method} to call for the circumstances
	 *                  represented by this {@link Connector}
	 */
	void connect(Object instance, Type<?> as, Method connected);

}
