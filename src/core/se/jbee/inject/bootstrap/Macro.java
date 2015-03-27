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
 * passed value into a {@link Module} that afterwards will be
 * {@link Module#declare(Bindings)} the {@link Binding}s that correspond to the
 * value.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <V>
 *            The type of value that is expanded by this macro
 */
public interface Macro<V> {

	/**
	 * Expands the incomplete {@link Binding} and value given to a
	 * {@link Module} that declares the complete {@link Binding}(s).
	 * 
	 * @param binding
	 *            A incomplete {@link Binding} (without a {@link Supplier})
	 * @param value
	 *            A {@link Class}, {@link Instance} or similar values that
	 *            express the intent of the incomplete binding. This
	 *            {@link Macro} will use them especially to decide the
	 *            {@link Supplier} used.
	 * @return The {@link Module} created by the macro that will declare the
	 *         complete {@link Binding}(s) that are used for the value. This
	 *         often is also a {@link Binding} (what is the simplest
	 *         {@link Module} as well)
	 */
	<T> Module expand( Binding<T> binding, V value );
}
