package de.jbee.inject;

import static de.jbee.inject.Source.source;

import java.util.List;
import java.util.Set;

/**
 * Installs all the build-in functionality by using the core API.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
final class BuildinModule
		implements Module {

	private static final Source BUILD_IN = source( BuildinModule.class );

	@Override
	public void configure( BindDeclarator binder ) {
		SimpleBinder bb = new SimpleBinder( binder, BUILD_IN, Scoped.DEFAULT );
		bb.wildcardBind( Provider.class, Suppliers.PROVIDER );
		bb.wildcardBind( List.class, Suppliers.LIST_BRIDGE );
		bb.wildcardBind( Set.class, Suppliers.SET_BRIDGE );
	}

}
