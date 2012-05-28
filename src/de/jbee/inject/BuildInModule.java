package de.jbee.inject;

import static de.jbee.inject.Source.source;

/**
 * Installs all the build-in functionality by using the core API.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public class BuildInModule
		implements Module {

	private static final Source BUILD_IN = source( BuildInModule.class );

	@Override
	public void configure( Binder binder ) {
		Resource<Provider> provider = Instance.anyOf(
				Type.rawType( Provider.class ).parametizedAsLowerBounds() ).toResource();
		binder.bind( provider, Suppliers.PROVIDER, Scoped.APPLICATION, BUILD_IN );

		// TODO further build-in binds
	}

}
