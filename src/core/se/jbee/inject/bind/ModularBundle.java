/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.bind.Bootstrapper.ModularBootstrapper;

public interface ModularBundle<M> {

	void bootstrap( ModularBootstrapper<M> bootstrap );
}
