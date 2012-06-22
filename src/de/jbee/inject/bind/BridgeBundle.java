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
		bootstrap.install( ListBridgeModule.class, LIST );
		bootstrap.install( SetBridgeModule.class, SET );
		bootstrap.install( ProviderBridgeModule.class, PROVIDER );
	}

	static class ProviderBridgeModule
			extends BinderModule {

		@Override
		protected void declare() {
			superbind( Provider.class ).to( Suppliers.PROVIDER_BRIDGE );
		}

	}

	static class ListBridgeModule
			extends BinderModule {

		@Override
		protected void declare() {
			superbind( List.class ).to( Suppliers.LIST_BRIDGE );
		}

	}

	static class SetBridgeModule
			extends BinderModule {

		@Override
		protected void declare() {
			superbind( Set.class ).to( Suppliers.SET_BRIDGE );
		}

	}
}