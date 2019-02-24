/*
 *  Copyright (c) 2012-2017, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

/**
 * A {@link PresetModule} is an extension to a usual {@link Module} that depends
 * on *one* of the values that have been preset.
 * 
 * @see Module
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <T> The type of the preset value
 */
@FunctionalInterface
public interface PresetModule<T> {

	/**
	 * @param bindings use to declare made bound within this {@link Module}.
	 * @param preset The preset value (chosen by the value's type from the set
	 *            of all preset values). This can very well be null in case no
	 *            such type value has been preset.
	 */
	void declare(Bindings bindings, T preset);
}
