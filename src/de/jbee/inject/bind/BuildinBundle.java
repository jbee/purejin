package de.jbee.inject.bind;

import java.util.List;
import java.util.Set;

import de.jbee.inject.Provider;
import de.jbee.inject.Suppliers;
import de.jbee.inject.bind.Bootstrapper.ModularBootstrapper;

/**
 * Installs all the build-in functionality by using the core API.
 */
public enum BuildinBundle
		implements ModularBundle<BuildinBundle> {
	PROVIDER,
	LIST,
	SET;

	@Override
	public void bootstrap( ModularBootstrapper<BuildinBundle> bootstrap ) {
		bootstrap.install( ListBridgeModule.class, LIST );
		bootstrap.install( SetBridgeModule.class, SET );
		bootstrap.install( ProviderBridgeModule.class, PROVIDER );
	}

	private static class ProviderBridgeModule
			extends BinderModule {

		@Override
		protected void declare() {
			superbind( Provider.class ).to( Suppliers.PROVIDER_BRIDGE );
		}

	}

	private static class ListBridgeModule
			extends BinderModule {

		@Override
		protected void declare() {
			superbind( List.class ).to( Suppliers.LIST_BRIDGE );
		}

	}

	private static class SetBridgeModule
			extends BinderModule {

		@Override
		protected void declare() {
			superbind( Set.class ).to( Suppliers.SET_BRIDGE );
		}

	}

}