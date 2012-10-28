/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package de.jbee.inject.bind;

import de.jbee.inject.Resource;
import de.jbee.inject.Scope;
import de.jbee.inject.Source;
import de.jbee.inject.Supplier;

/**
 * The binding-builders are just utilities to construct calls to
 * {@link #add(Resource, Supplier, Scope, Source)}
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Bindings {

	<T> void add( Resource<T> resource, Supplier<? extends T> supplier, Scope scope, Source source );
}
