package de.jbee.inject.bind;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import de.jbee.inject.bind.Bootstrapper.ModularBootstrapper;
import de.jbee.inject.util.Provider;
import de.jbee.inject.util.Suppliers;

/**
 * Installs all the build-in functionality by using the core API.
 */
public enum BuildinBundle
		implements ModularBundle<BuildinBundle> {
	PROVIDER,
	LIST,
	SET,
	LOGGER;

	@Override
	public void bootstrap( ModularBootstrapper<BuildinBundle> bootstrap ) {
		bootstrap.install( ListBridgeModule.class, LIST );
		bootstrap.install( SetBridgeModule.class, SET );
		bootstrap.install( ProviderBridgeModule.class, PROVIDER );
		bootstrap.install( LoggerModule.class, LOGGER );
	}

	private static class LoggerModule
			extends BinderModule {

		@Override
		protected void declare() {
			starbind( Logger.class ).to( Suppliers.LOGGER );
		}

	}

	private static class ProviderBridgeModule
			extends BinderModule {

		@Override
		protected void declare() {
			starbind( Provider.class ).to( Suppliers.PROVIDER_BRIDGE );
		}

	}

	private static class ListBridgeModule
			extends BinderModule {

		@Override
		protected void declare() {
			starbind( List.class ).to( Suppliers.LIST_BRIDGE );
		}

	}

	private static class SetBridgeModule
			extends BinderModule {

		@Override
		protected void declare() {
			starbind( Set.class ).to( Suppliers.SET_BRIDGE );
		}

	}

}