/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package de.jbee.inject.draft;

import de.jbee.inject.Dependency;

public interface Lifespan<T> {

	T origination( Dependency<?> dependency );

	Lifecycle cycle( T origination );

}
