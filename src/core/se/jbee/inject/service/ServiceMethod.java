/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package de.jbee.inject.service;

import de.jbee.inject.bind.Extension;

/**
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <R>
 *            The return type of the method wired to this service
 */
public interface ServiceMethod<P, R> {

	R invoke( P params );

	enum ServiceClassExtension
			implements Extension<ServiceClassExtension, Object> {
		// no different options
	}
}
