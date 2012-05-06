package de.jbee.inject.util;

import de.jbee.inject.Binder;
import de.jbee.inject.Module;

public abstract class PackageModule
		implements Module {

	private PresetBinder binder;

	@Override
	public final void configure( Binder binder ) {
		if ( this.binder != null ) {
			throw new IllegalStateException( "Reentrance not allowed!" );
		}
		this.binder = new RichPresetBinder( binder ); //FIXME create root binder here
		configure();
	}

	abstract void configure();
}
