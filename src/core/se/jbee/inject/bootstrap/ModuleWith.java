/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import se.jbee.inject.Injector;
import se.jbee.inject.config.Options;

/**
 * A {@link ModuleWith} is an extension to a usual {@link Module} that depends
 * on *one* of the values that have been set to {@link Options} used when
 * bootstrapping the {@link Injector}.
 * 
 * @see Module
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <T> The type of the {@link Options} value
 */
@FunctionalInterface
public interface ModuleWith<T> {

	/**
	 * @param bindings use to declare made bound within this {@link Module}.
	 * @param option The set value for the type (maybe null in case no value was
	 *            set or the value was set to null)
	 */
	void declare(Bindings bindings, T option);
}
