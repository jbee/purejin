/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.Source;
import se.jbee.inject.Supplier;

/**
 * The binding-builders are just utilities to construct calls to
 * {@link #add(Resource, Supplier, Scope, Source)}
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public interface Bindings {

	<T> void add( Resource<T> resource, Supplier<? extends T> supplier, Scope scope, Source source );
}
