/*
 *  Copyright (c) 2012-2019, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Bootstrapper.ToggledBootstrapper;
import se.jbee.inject.declare.Bundle;

/**
 * The default utility base class for {@link ToggledBundles}s.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param O the type of the options values (usually an enum)
 */
public abstract class ToggledBootstrapperBundle<C>
		implements ToggledBundles<C>, ToggledBootstrapper<C> {

	private ToggledBootstrapper<C> bootstrapper;

	@Override
	public void bootstrap(ToggledBootstrapper<C> bs) {
		Bootstrap.nonnullThrowsReentranceException(bootstrapper);
		this.bootstrapper = bs;
		bootstrap();
	}

	@Override
	public void install(Class<? extends Bundle> bundle, C flag) {
		bootstrapper.install(bundle, flag);
	}

	@Override
	public String toString() {
		Type<?> module = Type.supertype(ToggledBundles.class,
				Type.raw(getClass())).parameter(0);
		return "bundle " + getClass().getSimpleName() + "[" + module + "]";
	}

	/**
	 * Use {@link #install(Class, Object)} for option dependent {@link Bundle}
	 * installation.
	 */
	protected abstract void bootstrap();
}
