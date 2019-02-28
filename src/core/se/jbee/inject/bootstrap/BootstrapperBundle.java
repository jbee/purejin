/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import se.jbee.inject.config.ConstructionMirror;
import se.jbee.inject.config.NamingMirror;
import se.jbee.inject.config.ParameterisationMirror;
import se.jbee.inject.config.ProductionMirror;

/**
 * The default utility {@link Bundle} that is a {@link Bootstrap} as well so
 * that bindings can be declared nicer.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class BootstrapperBundle implements Bundle, Bootstrapper {

	private Bootstrapper bootstrap;

	@Override
	public final void bootstrap(Bootstrapper bootstrap) {
		Bootstrap.nonnullThrowsReentranceException(this.bootstrap);
		this.bootstrap = bootstrap;
		bootstrap();
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
	public final <T> void install(PresetModule<T> module) {
		bootstrap.install(module);
	}

	@Override
	public final void uninstall(Class<? extends Bundle> bundle) {
		bootstrap.uninstall(bundle);
	}

	@Override
	@SafeVarargs
	public final <M extends Enum<M> & OptionBundle<M>> void install(
			M... modules) {
		bootstrap.install(modules);
	}

	@Override
	public final <C extends Enum<C>> void install(
			Class<? extends OptionBundle<C>> bundle, Class<C> property) {
		bootstrap.install(bundle, property);
	}

	@Override
	@SafeVarargs
	public final <O extends Enum<O> & OptionBundle<O>> void uninstall(
			O... options) {
		bootstrap.uninstall(options);
	}

	protected final <O extends Enum<O> & OptionBundle<O>> void installAll(
			Class<O> optionsOfType) {
		install(optionsOfType.getEnumConstants());
	}

	protected final <O extends Enum<O> & OptionBundle<O>> void uninstallAll(
			Class<O> optionsOfType) {
		uninstall(optionsOfType.getEnumConstants());
	}

	protected final void install(Module module, ConstructionMirror mirror) {
		install(bindings -> module.declare(bindings.with(mirror)));
	}

	protected final void install(Class<? extends Module> module,
			ConstructionMirror mirror) {
		install(newInstance(module), mirror);
	}

	protected final void install(Module module, NamingMirror mirror) {
		install(bindings -> module.declare(bindings.with(mirror)));
	}

	protected final void install(Class<? extends Module> module,
			NamingMirror mirror) {
		install(newInstance(module), mirror);
	}

	protected final void install(Module module, ProductionMirror mirror) {
		install(bindings -> module.declare(bindings.with(mirror)));
	}

	protected final void install(Class<? extends Module> module,
			ProductionMirror mirror) {
		install(newInstance(module), mirror);
	}

	protected final void install(Module module, ParameterisationMirror mirror) {
		install(bindings -> module.declare(bindings.with(mirror)));
	}

	protected final void install(Class<? extends Module> module,
			ParameterisationMirror mirror) {
		install(newInstance(module), mirror);
	}

	protected static Module newInstance(Class<? extends Module> module) {
		return Bootstrap.instance(module);
	}

	@Override
	public String toString() {
		return "bundle " + getClass().getSimpleName();
	}

	protected abstract void bootstrap();

}
