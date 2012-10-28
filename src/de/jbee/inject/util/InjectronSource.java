/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package de.jbee.inject.util;

import de.jbee.inject.Injector;
import de.jbee.inject.Injectron;

/**
 * A factory like origin for {@link Injectron}s.
 * 
 * This is a abstraction on a concrete setup.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 */
public interface InjectronSource {

	/**
	 * @return A list of {@link Injectron}s suitable for the {@link Injector} given. That mean will
	 *         become the {@link Injector}s basis and therefore share the same view and data.
	 */
	Injectron<?>[] exportTo( Injector injector );
}
