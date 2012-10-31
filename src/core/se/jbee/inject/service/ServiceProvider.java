/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.service;

import se.jbee.inject.Type;

public interface ServiceProvider {

	<P, R> ServiceMethod<P, R> provide( Type<P> parameterType, Type<R> returnType );
}
