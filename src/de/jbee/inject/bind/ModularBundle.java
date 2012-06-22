package de.jbee.inject.bind;

import de.jbee.inject.bind.Bootstrapper.ModularBootstrapper;

public interface ModularBundle<M> {

	void bootstrap( ModularBootstrapper<M> bootstrap );
}
