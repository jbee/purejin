/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package de.jbee.inject.bind;

import de.jbee.inject.bind.Bootstrapper.ModularBootstrapper;

public interface ModularBundle<M> {

	void bootstrap( ModularBootstrapper<M> bootstrap );
}
