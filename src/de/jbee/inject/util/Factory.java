/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package de.jbee.inject.util;

import de.jbee.inject.Instance;

public interface Factory<T> {

	<P> T produce( Instance<? super T> produced, Instance<P> injected );
}
