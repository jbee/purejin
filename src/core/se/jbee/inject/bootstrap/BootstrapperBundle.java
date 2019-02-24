/*
 *  Copyright (c) 2012-2017, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

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

	/**
	 * Installs the given {@link Module} using the given {@link Inspector} when
	 * declaring binds.
	 */
	protected final void install(Module module, Inspector inspector) {
		install(new InspectorModule(module, inspector));
	}

	protected final void install(Class<? extends Module> module,
			Inspector inspector) {
		install(newInstance(module), inspector);
	}

	protected static Module newInstance(Class<? extends Module> module) {
		return Bootstrap.instance(module);
	}

	@Override
	public String toString() {
		return "bundle " + getClass().getSimpleName();
	}

	protected abstract void bootstrap();

	private static final class InspectorModule implements Module {

		private final Module module;
		private final Inspector inspector;

		InspectorModule(Module module, Inspector inspector) {
			this.module = module;
			this.inspector = inspector;
		}

		@Override
		public void declare(Bindings bindings) {
			module.declare(bindings.using(inspector));
		}

		@Override
		public String toString() {
			return module + "[" + inspector + "]";
		}
	}
}
