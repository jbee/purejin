/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.action;

import java.lang.reflect.Method;

import se.jbee.inject.Type;

@FunctionalInterface
public interface Executor {

	<I,O> O exec(Object impl, Method action, Object[] args, Type<O> output, Type<I> input, I value) throws ActionMalfunction;
}
