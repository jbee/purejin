/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;


/**
 * This about the HOW to inject (field, constructor/setter)
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 */
public interface InjectionPoint<T> {

	void inject( T instance );

	Instance<T> getInstance();

	interface InjectionPointStrategy {

		<T> InjectionPoint<?>[] injectionPointsFor( Class<T> type );
	}
}
