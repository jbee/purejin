/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.binder;

import se.jbee.inject.bind.Bootstrapper;
import se.jbee.inject.bind.Bootstrapper.DependentBootstrapper;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.Dependent;
import se.jbee.inject.bind.InconsistentBinding;
import se.jbee.lang.Type;

import static se.jbee.lang.Type.raw;

/**
 * The default utility base class for {@link Dependent}s.
 *
 * @param <E> the type of the options values (usually an enum)
 */
public abstract class BundleFor<E> implements Dependent<E>,
		Bootstrapper.DependentBootstrapper<E> {

	private DependentBootstrapper<E> bootstrapper;

	@Override
	public void bootstrap(Bootstrapper.DependentBootstrapper<E> bs) {
		InconsistentBinding.nonnullThrowsReentranceException(bootstrapper);
		this.bootstrapper = bs;
		bootstrap();
	}

	@Override
	public final void installDependentOn(E element, Class<? extends Bundle> bundle) {
		bootstrapper.installDependentOn(element, bundle);
	}

	@Override
	public final String toString() {
		Type<?> module = raw(getClass()).toSuperType(Dependent.class).parameter(0);
		return "bundle " + getClass().getSimpleName() + "[" + module + "]";
	}

	/**
	 * Use {@link DependentBootstrapper#installDependentOn(Object, Class)} for
	 * option dependent {@link Bundle} installation.
	 */
	protected abstract void bootstrap();
}
