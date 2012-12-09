/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.util;

import se.jbee.inject.Instance;
import se.jbee.inject.Supplier;

/**
 * A slightly more high level abstraction than a {@link Supplier} purely for ease of implementation
 * of <i>sources</i> that behave like a classical factory (pattern).
 * 
 * @see Supplier
 * @see Provider
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <T>
 *            The type of value produced
 */
public interface Factory<T> {

	/**
	 * @param <P>
	 *            The type of the receiving instance.
	 * @param produced
	 *            Describes what should be produced.
	 * @param injected
	 *            Describes the instance that receives the produced instance.
	 * @return produced instance
	 */
	<P> T produce( Instance<? super T> produced, Instance<P> injected );
}
