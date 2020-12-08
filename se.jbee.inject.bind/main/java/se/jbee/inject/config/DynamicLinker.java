package se.jbee.inject.config;

import java.lang.reflect.Method;

@FunctionalInterface
public interface DynamicLinker {

	/**
	 *
	 * @param instance the subject instance that should receive the call
	 * @param linked the {@link Method} to call for the circumstances represented by this {@link DynamicLinker}
	 */
	void link(Object instance, Method linked);


	//TODO add way to call the method with injection and some other arguments the way actions do
	// and implement actions with linkers

	//TODO add a default bind for a DynamicLinker of any name that just logs a warning
	// Action then has to work as a lazy proxy as one might inject Actions before we know about the method and instance that implement them
	// also this might allow for ambiguity where multiple instances offer the target...(this was already true but ignored)
}
