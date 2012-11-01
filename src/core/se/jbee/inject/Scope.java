/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * A {@linkplain Scope} describes a particular lifecycle.
 * 
 * Thereby the {@linkplain Scope} itself acts as a factory for {@link Repository}s. Each
 * {@link Injector} has a single {@linkplain Repository} for each {@linkplain Scope}.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Scope {

	/**
	 * Creates a empty {@link Repository} that stores instances in this {@link Scope}.
	 * 
	 * @return a empty instance in this {@linkplain Scope}.
	 */
	Repository init();
}
