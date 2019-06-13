/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

/**
 * A {@link Constant} is the {@link Macro} expansion wrapper type for any
 * constant bound to in the fluent binder API.
 * 
 * @param <T> Type of the constant value
 */
public final class Constant<T> {

	public final T constant;

	public Constant(T constant) {
		this.constant = constant;
	}

}
