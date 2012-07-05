package de.jbee.inject.bind;

import static de.jbee.inject.bind.Bootstrap.nonnullThrowsReentranceException;

public abstract class DirectBundle
		//TODO better name
		implements Bundle, Bootstrapper {

	private Bootstrapper bootstrap;

	@Override
	public final void bootstrap( Bootstrapper bootstrap ) {
		nonnullThrowsReentranceException( this.bootstrap );
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
	public final void uninstall( Class<? extends Bundle> bundle ) {
		bootstrap.uninstall( bundle );
	}

	@Override
	public <M extends Enum<M> & ModularBundle<M>> void install( M... modules ) {
		bootstrap.install( modules );
	}

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
