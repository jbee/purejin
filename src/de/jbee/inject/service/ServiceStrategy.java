package de.jbee.inject.service;

import java.lang.reflect.Method;

public interface ServiceStrategy {

	/**
	 * Allows to customize what methods are bound as {@link ServiceMethod}s.
	 */
	Method[] serviceMethodsIn( Class<?> serviceClass );
}
