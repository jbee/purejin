/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

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
	public <T> void install( PresetModule<T> module ) {
		bootstrap.install( module );
	}

	@Override
	public final void uninstall( Class<? extends Bundle> bundle ) {
		bootstrap.uninstall( bundle );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <M extends Enum<M> & ModularBundle<M>> void install( M... modules ) {
		bootstrap.install( modules );
	}

	@Override
	public <C extends Enum<C>> void install( Class<? extends ModularBundle<C>> bundle,
			Class<C> property ) {
		bootstrap.install( bundle, property );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <M extends Enum<M> & ModularBundle<M>> void uninstall( M... modules ) {
		bootstrap.uninstall( modules );
	}

	protected final <M extends Enum<M> & ModularBundle<M>> void installAll( Class<M> modules ) {
		install( modules.getEnumConstants() );
	}

	protected final <M extends Enum<M> & ModularBundle<M>> void uninstallAll( Class<M> modules ) {
		uninstall( modules.getEnumConstants() );
	}

	protected abstract void bootstrap();
}
