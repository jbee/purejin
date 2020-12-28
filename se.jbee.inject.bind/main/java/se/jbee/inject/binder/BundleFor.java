/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.binder;

import se.jbee.inject.bind.Bootstrapper;
import se.jbee.inject.bind.Bootstrapper.ToggledBootstrapper;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.InconsistentBinding;
import se.jbee.inject.bind.Toggled;
import se.jbee.inject.lang.Type;

import static se.jbee.inject.lang.Type.raw;

/**
 * The default utility base class for {@link Toggled}s.
 *
 * @param <C> the type of the options values (usually an enum)
 */
public abstract class BundleFor<C> implements Toggled<C>,
		ToggledBootstrapper<C> {

	private Bootstrapper.ToggledBootstrapper<C> bootstrapper;

	@Override
	public void bootstrap(Bootstrapper.ToggledBootstrapper<C> bs) {
		InconsistentBinding.nonnullThrowsReentranceException(bootstrapper);
		this.bootstrapper = bs;
		bootstrap();
	}

	@Override
	public final void install(Class<? extends Bundle> bundle, C flag) {
		bootstrapper.install(bundle, flag);
	}

	@Override
	public final String toString() {
		Type<?> module = raw(getClass()).toSuperType(Toggled.class).parameter(0);
		return "bundle " + getClass().getSimpleName() + "[" + module + "]";
	}

	/**
	 * Use {@link #install(Class, Object)} for option dependent {@link Bundle}
	 * installation.
	 */
	protected abstract void bootstrap();
}
