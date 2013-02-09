/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.bind.Bootstrapper.ModularBootstrapper;

/**
 * The default ustility base class for {@link ModularBundle}s.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class ModularBootstrapperBundle<M>
		implements ModularBundle<M>, ModularBootstrapper<M> {

	private ModularBootstrapper<M> bootstrap;

	@Override
	public void bootstrap( ModularBootstrapper<M> bootstrap ) {
		Bootstrap.nonnullThrowsReentranceException( this.bootstrap );
		this.bootstrap = bootstrap;
		bootstrap();
	}

	@Override
	public void install( Class<? extends Bundle> bundle, M module ) {
		bootstrap.install( bundle, module );
	}

	protected abstract void bootstrap();
}
