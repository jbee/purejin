/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.container;

import java.util.List;

import se.jbee.inject.Generator;
import se.jbee.inject.InjectionCase;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.Resource;
import se.jbee.inject.Source;

/**
 * A "construction description" for a particular instance or set of kind of
 * instances.
 * 
 * A {@link List} would be a generic {@link Injectee}, some singleton "service"
 * a non-generic one.
 * 
 * Each {@link Injectee} becomes an {@link InjectionCase} and {@link Generator}
 * within the {@link Injector} (1:1 relation).
 * 
 * The interface is mainly introduced to decouple the everything on top of the
 * container module (which is the core) from the container implementation
 * itself. In the concrete case the bootstrap package should only depend on the
 * container but not vice versa. This also allows to build custom bootstrapping
 * (utility) packages as only contract is a consistent list of {@link Injectee}
 * is produced to build a {@link Injector}s context.
 * 
 * @since 19.1
 * 
 * @param <T> Type of instances assembled.
 */
public interface Injectee<T> {

	Source source();

	Name scope();

	Resource<T> resource();

	Supplier<? extends T> supplier();

}
