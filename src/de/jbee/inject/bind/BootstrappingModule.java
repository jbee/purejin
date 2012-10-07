package de.jbee.inject.bind;

public abstract class BootstrappingModule
		implements Module, Bundle {

	@Override
	public final void bootstrap( Bootstrapper bootstrap ) {
		bootstrap.install( this );
	}

	public static void nonnullThrowsReentranceException( Object field ) {
		if ( field != null ) {
			throw new IllegalStateException( "Reentrance not allowed!" );
		}
	}
}
