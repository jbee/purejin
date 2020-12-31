/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.binder;

import se.jbee.inject.bind.Module;
import se.jbee.inject.bind.*;

/**
 * The default utility {@link Bundle} that is a {@link Bootstrapper} as well so
 * that bindings can be declared nicer.
 */
public abstract class BootstrapperBundle implements Bundle, Bootstrapper {

	private Bootstrapper bootstrap;

	@Override
	public final void bootstrap(Bootstrapper bootstrap) {
		InconsistentBinding.nonnullThrowsReentranceException(this.bootstrap);
		this.bootstrap = bootstrap;
		bootstrap();
	}

	@Override
	public void installDefaults() {
		bootstrap.installDefaults();
	}

	@Override
	public final void install(Class<? extends Bundle> bundle) {
		bootstrap.install(bundle);
	}

	@Override
	public final void install(Module module) {
		bootstrap.install(module);
	}

	@Override
	public final void uninstall(Class<? extends Bundle> bundle) {
		bootstrap.uninstall(bundle);
	}

	@Override
	@SafeVarargs
	public final <M extends Enum<M> & Dependent<M>> void install(M... elements) {
		bootstrap.install(elements);
	}

	@Override
	public final <C extends Enum<C>> void install(
			Class<? extends Dependent<C>> bundle, Class<C> dependentOn) {
		bootstrap.install(bundle, dependentOn);
	}

	@Override
	@SafeVarargs
	public final <O extends Enum<O> & Dependent<O>> void uninstall(O... elements) {
		bootstrap.uninstall(elements);
	}

	protected final <O extends Enum<O> & Dependent<O>> void installAll(
			Class<O> optionsOfType) {
		install(optionsOfType.getEnumConstants());
	}

	protected final <O extends Enum<O> & Dependent<O>> void uninstallAll(
			Class<O> optionsOfType) {
		uninstall(optionsOfType.getEnumConstants());
	}

	@Override
	public String toString() {
		return "bundle " + getClass().getSimpleName();
	}

	protected abstract void bootstrap();

}
