/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

/**
 * The default utility {@link Bundle} that is a {@link Bootstrap} as well so that bindings can be
 * declared nicer.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class BootstrapperBundle
		implements Bundle, Bootstrapper {

	private Bootstrapper bootstrap;

	@Override
	public final void bootstrap( Bootstrapper bootstrap ) {
		Bootstrap.nonnullThrowsReentranceException( this.bootstrap );
		this.bootstrap = bootstrap;
		bootstrap();
	}

	@Override
	public final void install( Class<? extends Bundle> bundle ) {
		bootstrap.install( bundle );
	}

	@Override
	public final void install( Module module ) {
		bootstrap.install( module );
	}

	@Override
	public final <T> void install( PresetModule<T> module ) {
		bootstrap.install( module );
	}

	@Override
	public final void uninstall( Class<? extends Bundle> bundle ) {
		bootstrap.uninstall( bundle );
	}

	@Override
	public final <M extends Enum<M> & ModularBundle<M>> void install( M... modules ) {
		bootstrap.install( modules );
	}

	@Override
	public final <C extends Enum<C>> void install( Class<? extends ModularBundle<C>> bundle,
			Class<C> property ) {
		bootstrap.install( bundle, property );
	}

	@Override
	public final <M extends Enum<M> & ModularBundle<M>> void uninstall( M... modules ) {
		bootstrap.uninstall( modules );
	}

	protected final <M extends Enum<M> & ModularBundle<M>> void installAll( Class<M> modules ) {
		install( modules.getEnumConstants() );
	}

	protected final <M extends Enum<M> & ModularBundle<M>> void uninstallAll( Class<M> modules ) {
		uninstall( modules.getEnumConstants() );
	}

	/**
	 * Installs the given {@link Module} using the given {@link Inspector} when declaring binds.
	 */
	protected final void install( Module module, Inspector inspector ) {
		install( new InspectorModule( module, inspector ) );
	}

	protected final void install( Class<? extends Module> module, Inspector inspector ) {
		install( newInstance( module ), inspector );
	}

	protected static Module newInstance( Class<? extends Module> module ) {
		return Bootstrap.instance( module );
	}

	@Override
	public String toString() {
		return "bundle " + getClass().getSimpleName();
	}

	protected abstract void bootstrap();

	private static final class InspectorModule
			implements Module {

		private final Module module;
		private final Inspector inspector;

		InspectorModule( Module module, Inspector inspector ) {
			super();
			this.module = module;
			this.inspector = inspector;
		}

		@Override
		public void declare( Bindings bindings, Inspector inspector ) {
			module.declare( bindings, this.inspector );
		}

		@Override
		public String toString() {
			return module + "[" + inspector + "]";
		}
	}
}
