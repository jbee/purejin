/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Bootstrapper.ModularBootstrapper;

/**
 * The default utility base class for {@link ModularBundle}s.
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

	@Override
	public String toString() {
		Type<?> module = Type.supertype( ModularBundle.class, Type.raw( getClass() ) ).parameter( 0 );
		return "bundle " + getClass().getSimpleName() + "[" + module + "]";
	}

	protected abstract void bootstrap();
}
