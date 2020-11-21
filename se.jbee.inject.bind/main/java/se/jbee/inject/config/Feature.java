/*
 *  Copyright (c) 2012-2019, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

/**
 * {@link Feature}s can be used to model more fine grained {@link Edition} by
 * using <code>enum</code>s as the options to chose from.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <T> The enum used as different features/options to chose from.
 */
@FunctionalInterface
public interface Feature<T extends Enum<T>> {

	/**
	 * @return The feature this given bundle or module class represents or
	 *         <code>null</code> is it doesn't represent any special feature (so
	 *         it will be install in any case).
	 */
	T featureOf(Class<?> bundleOrModule);
}
