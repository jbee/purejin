/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import se.jbee.inject.Instance;
import se.jbee.inject.Supplier;

/**
 * A {@linkplain Macro} is a prepared set of instructions that transform the
 * passed value into a one or more complete {@link Binding}s that are added to
 * the set of {@link Bindings}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <V>
 *            The type of value that is expanded by this macro
 */
@FunctionalInterface
public interface Macro<V> {

	/**
	 * Expands the incomplete {@link Binding} and value given to a complete
	 * {@link Binding}(s) that are added to {@link Bindings}.
	 * 
	 * @param value
	 *            A {@link Class}, {@link Instance} or similar values that
	 *            express the intent of the incomplete binding. This
	 *            {@link Macro} will use them especially to decide the
	 *            {@link Supplier} used.
	 * @param incomplete
	 *            A usually incomplete {@link Binding} (without a
	 *            {@link Supplier})
	 * @param bindings
	 *            The collection of {@link Bindings} complete {@link Binding}s
	 *            should be added to.
	 */
	<T> void expand(V value, Binding<T> incomplete, Bindings bindings);
}
