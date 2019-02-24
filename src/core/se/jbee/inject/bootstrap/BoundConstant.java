/*
 *  Copyright (c) 2012-2019, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

public final class BoundConstant<T> {

	public final T constant;

	public BoundConstant(T constant) {
		this.constant = constant;
	}

}
