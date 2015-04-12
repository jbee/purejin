/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * If there is a statically resolvable problem with a binding (resource in
 * the context of a container) this exception is thrown during
 * bootstrapping. It is never thrown after the bootstrapping step has
 * finished (a {@link Injector} was created successfully).
 * 
 * @see UnresolvableDependency
 */
public final class InconsistentBinding extends RuntimeException {

	public InconsistentBinding(String message) {
		super(message);
	}
	
}