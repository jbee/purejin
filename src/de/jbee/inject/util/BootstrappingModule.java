package de.jbee.inject.util;

import de.jbee.inject.Bootstrapper;
import de.jbee.inject.Bundle;
import de.jbee.inject.Module;

public abstract class BootstrappingModule
		implements Module, Bundle {

	@Override
	public final void bootstrap( Bootstrapper bootstrap ) {
		bootstrap.install( this );
	}
}
