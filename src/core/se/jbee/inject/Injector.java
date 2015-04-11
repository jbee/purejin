/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import se.jbee.inject.UnresolvableDependency.DependencyCycle;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.UnresolvableDependency.UnstableDependency;

/**
 * Knows how to *resolve* an instance for a given {@link Dependency}.
 * 
 * The process of resolving might contain fabrication of instances.
 * 
 * A {@link Injector} is immutable (at least from the outside view). Once created it provides a
 * certain set of supported dependencies that can be resolved. All calls to
 * {@link #resolve(Dependency)} always have the same result for the same {@linkplain Dependency}.
 * The only exception to this are scoping effects (expiring and parallel instances).
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public interface Injector {

	/**
	 * @return Resolves the instance appropriate for the given {@link Dependency}. In case no such
	 *         instance is known an exception is thrown. The <code>null</code>-reference will never
	 *         be returned.
	 * @throws NoResourceForDependency
	 *             In case no {@link Resource} in this injector's context matches the given
	 *             dependency.
	 * @throws UnstableDependency
	 *             In case the resolution would cause the injection of a instance into another that
	 *             has a higher {@link Expiry}.
	 * @throws DependencyCycle
	 *             In case the resolution caused a situation of cyclic dependent instances so that
	 *             they cannot be injected.
	 */
	<T> T resolve( Dependency<T> dependency ) throws UnresolvableDependency;
}
