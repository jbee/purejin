/*
 *  Copyright (c) 2012-2017, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;


/**
 * Knows how to *resolve* an instance for a given {@link Dependency}.
 * 
 * The process of resolving might include fabrication of instances.
 * 
 * An {@link Injector} is immutable (at least from the outside view). Once
 * created it provides a certain set of supported dependencies that can be
 * resolved. All calls to {@link #resolve(Dependency)} always have the same
 * result for the same {@linkplain Dependency}. The only exception to this are
 * scoping effects (expiring and parallel instances).
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
@FunctionalInterface
public interface Injector {

	<T> T resolve( Dependency<T> dependency ) throws UnresolvableDependency;
}
