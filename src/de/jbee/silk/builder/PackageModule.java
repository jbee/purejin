package de.jbee.silk.builder;

import de.jbee.silk.Binder;
import de.jbee.silk.Module;

public abstract class PackageModule
		implements Module {

	private PresetBinder binder;

	@Override
	public final void configure( Binder binder ) {
		if ( this.binder != null ) {
			throw new IllegalStateException( "Reentrance not allowed!" );
		}
		this.binder = new PresetCoreBinder( binder ); //FIXME create root binder here
		configure();
	}

	abstract void configure();
}
