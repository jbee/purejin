/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * {@link Bindings} are defined with {@link Module}s while {@link Bundle} are used to group multiple
 * {@link Module}s and {@link Bundle}s what allows to build graphs of {@link Bundle}s with
 * {@link Module}s as leafs.
 * 
 * @see Bundle
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Module {

	/**
	 * @param bindings
	 *            use to declare made bound within this {@link Module}.
	 * @param strategy
	 *            the chosen strategy to pick the use {@link Constructor}s and {@link Method} used
	 *            to create instances.
	 */
	void declare( Bindings bindings, ConstructionStrategy strategy );

}
