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
 * {@link Bindings} are an abstraction to a consumer of a binding described by a 4-tuple. They are
 * usually accumulated within.
 * 
 * Any builder is just a utility to construct calls to
 * {@link #add(Resource, Supplier, Scope, Source)}
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public interface Bindings {

	/**
	 * Add (accumulate) a binding described by the 4-tuple given.
	 * 
	 * @param resource
	 *            describes the identity of the resulting instance(s)
	 * @param supplier
	 *            creates this instance(s)
	 * @param scope
	 *            describes and controls the life-cycle of the instance(s)
	 * @param source
	 *            describes the origin of the binding (this call) and its meaning
	 */
	<T> void add( Resource<T> resource, Supplier<? extends T> supplier, Scope scope, Source source );
}
