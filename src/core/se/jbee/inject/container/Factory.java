/*
 *  Copyright (c) 2012-2017, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.container;

import se.jbee.inject.Instance;
import se.jbee.inject.Supplier;
import se.jbee.inject.UnresolvableDependency;

/**
 * A slightly more high level abstraction than a {@link Supplier} purely for
 * ease of implementation of <i>sources</i> that behave like a classical factory
 * (pattern).
 * 
 * @see Supplier
 * @see Provider
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <T>
 *            The type of value created
 */
@FunctionalInterface
public interface Factory<T> {

	/**
	 * @param <P>
	 *            The type of the receiving instance.
	 * @param created
	 *            Describes what should be created.
	 * @param receiver
	 *            Describes the instance that receives the created instance.
	 * @return created instance (note that is does not throw an
	 *         {@link UnresolvableDependency} exception!)
	 */
	<P> T fabricate( Instance<? super T> created, Instance<P> receiver );
}
