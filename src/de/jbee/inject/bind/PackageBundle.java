package de.jbee.inject.bind;

import de.jbee.inject.bind.Bootstrap.CoreModule;

public abstract class PackageBundle
		implements Bundle, Bootstrapper {

	private Bootstrapper bootstrap;

	@Override
	public final void bootstrap( Bootstrapper bootstrap ) {
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

	protected final void install( CoreModule... modules ) {
		Bootstrap.install( bootstrap, modules );
	}

	protected abstract void bootstrap();
}
