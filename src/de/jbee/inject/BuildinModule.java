package de.jbee.inject;

import static de.jbee.inject.Source.source;

import java.util.List;
import java.util.Set;

import de.jbee.inject.util.BootstrappingModule;

/**
 * Installs all the build-in functionality by using the core API.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public final class BuildinModule
		extends BootstrappingModule {

	@Override
	public void configure( Bindings bindings ) {
		SimpleBinder bb = new SimpleBinder( bindings, source( BuildinModule.class ), Scoped.DEFAULT );
		bb.wildcardBind( Provider.class, Suppliers.PROVIDER );
		bb.wildcardBind( List.class, Suppliers.LIST_BRIDGE );
		bb.wildcardBind( Set.class, Suppliers.SET_BRIDGE );
	}

}
