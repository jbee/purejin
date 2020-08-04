/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.binder;

import se.jbee.inject.Type;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.InconsistentBinding;
import se.jbee.inject.bind.Toggled;
import se.jbee.inject.bind.Bootstrapper.Toggler;

/**
 * The default utility base class for {@link Toggled}s.
 *
 * @author Jan Bernitt (jan@jbee.se)
 *
 * @param <C> the type of the options values (usually an enum)
 */
public abstract class TogglerBundle<C> implements Toggled<C>, Toggler<C> {

	private Toggler<C> bootstrapper;

	@Override
	public void bootstrap(Toggler<C> bs) {
		InconsistentBinding.nonnullThrowsReentranceException(bootstrapper);
		this.bootstrapper = bs;
		bootstrap();
	}

	@Override
	public void install(Class<? extends Bundle> bundle, C flag) {
		bootstrapper.install(bundle, flag);
	}

	@Override
	public String toString() {
		Type<?> module = Type.supertype(Toggled.class,
				Type.raw(getClass())).parameter(0);
		return "bundle " + getClass().getSimpleName() + "[" + module + "]";
	}

	/**
	 * Use {@link #install(Class, Object)} for option dependent {@link Bundle}
	 * installation.
	 */
	protected abstract void bootstrap();
}
