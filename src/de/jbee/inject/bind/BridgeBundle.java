package de.jbee.inject.bind;

import java.util.List;
import java.util.Set;

import de.jbee.inject.Provider;
import de.jbee.inject.Suppliers;
import de.jbee.inject.bind.Bootstrapper.ModularBootstrapper;

/**
 * Installs all the build-in functionality by using the core API.
 */
public enum BridgeBundle
		implements ModularBundle<BridgeBundle> {
	PROVIDER,
	LIST,
	SET;

	@Override
	public void bootstrap( ModularBootstrapper<BridgeBundle> bootstrap ) {
		bootstrap.install( BridgeBundle.ListBridgeModule.class, LIST );
		bootstrap.install( BridgeBundle.SetBridgeModule.class, SET );
		bootstrap.install( BridgeBundle.ProviderBridgeModule.class, PROVIDER );
	}

	static class ProviderBridgeModule
			extends PackageModule {

		@Override
		protected void configure() {
			superbind( Provider.class ).to( Suppliers.PROVIDER );
		}

	}

	static class ListBridgeModule
			extends PackageModule {

		@Override
		protected void configure() {
			superbind( List.class ).to( Suppliers.LIST_BRIDGE );
		}

	}

	static class SetBridgeModule
			extends PackageModule {

		@Override
		protected void configure() {
			superbind( Set.class ).to( Suppliers.SET_BRIDGE );
		}

	}
}