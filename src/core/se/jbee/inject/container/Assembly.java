/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.container;

import java.util.List;

import se.jbee.inject.Resource;
import se.jbee.inject.Source;
import se.jbee.inject.Supplier;

/**
 * A concrete or abstract construction plan for a particular instance or a class
 * of instances.
 * 
 * Like {@link List} would be a generic assembly and some singleton a concrete.
 * 
 * The interface is mainly introduced to decouple the everything on top of the
 * container from the container implementation itself. In the concrete case the
 * bootstrap package should only depend on the container but not vice versa.
 * 
 * @param <T>
 *            Type of instances assembled.
 */
public interface Assembly<T> {

	Source source();
	
	Resource<T> resource();
	
	Scope scope();
	
	Supplier<? extends T> supplier();
	
}
