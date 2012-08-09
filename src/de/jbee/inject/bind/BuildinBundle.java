package de.jbee.inject.bind;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import de.jbee.inject.bind.Bootstrapper.ModularBootstrapper;
import de.jbee.inject.util.Provider;
import de.jbee.inject.util.Scoped;
import de.jbee.inject.util.SuppliedBy;

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
			per( Scoped.TARGET_INSTANCE ).starbind( Logger.class ).to( SuppliedBy.LOGGER );
		}

	}

	private static class ProviderBridgeModule
			extends BinderModule {

		@Override
		protected void declare() {
			per( Scoped.DEPENDENCY_TYPE ).starbind( Provider.class ).to( SuppliedBy.PROVIDER_BRIDGE );
		}

	}

	private static class ListBridgeModule
			extends BinderModule {

		@Override
		protected void declare() {
			per( Scoped.DEPENDENCY_TYPE ).starbind( List.class ).to( SuppliedBy.LIST_BRIDGE );
		}

	}

	private static class SetBridgeModule
			extends BinderModule {

		@Override
		protected void declare() {
			per( Scoped.DEPENDENCY_TYPE ).starbind( Set.class ).to( SuppliedBy.SET_BRIDGE );
		}

	}

}