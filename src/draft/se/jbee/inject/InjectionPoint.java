/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package de.jbee.inject.draft;

import de.jbee.inject.Instance;

/**
 * This about the HOW to inject (field, constructor/setter)
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 */
public interface InjectionPoint<T> {

	void inject( T instance );

	Instance<T> getInstance();

	interface InjectionPointStrategy {

		<T> InjectionPoint<?>[] injectionPointsFor( Class<T> type );
	}
}
