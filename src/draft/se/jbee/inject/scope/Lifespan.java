/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.scope;

import se.jbee.inject.Dependency;

public interface Lifespan<T> {

	T origination( Dependency<?> dependency );

	Lifecycle cycle( T origination );

}
