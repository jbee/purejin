/*
 *  Copyright (c) 2012-2019, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Bootstrapper.ChoiceBootstrapper;

/**
 * The default utility base class for {@link ChoiceBundle}s.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param O the type of the options values (usually an enum)
 */
public abstract class ChoiceBootstrapperBundle<C>
		implements ChoiceBundle<C>, ChoiceBootstrapper<C> {

	private ChoiceBootstrapper<C> bootstrapper;

	@Override
	public void bootstrap(ChoiceBootstrapper<C> bs) {
		Bootstrap.nonnullThrowsReentranceException(bootstrapper);
		this.bootstrapper = bs;
		bootstrap();
	}

	@Override
	public void install(Class<? extends Bundle> bundle, C choice) {
		bootstrapper.install(bundle, choice);
	}

	@Override
	public String toString() {
		Type<?> module = Type.supertype(ChoiceBundle.class,
				Type.raw(getClass())).parameter(0);
		return "bundle " + getClass().getSimpleName() + "[" + module + "]";
	}

	/**
	 * Use {@link #install(Class, Object)} for option dependent {@link Bundle}
	 * installation.
	 */
	protected abstract void bootstrap();
}
