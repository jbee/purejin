package de.jbee.inject.bind;

import static de.jbee.inject.util.Scoped.DEPENDENCY_TYPE;
import static de.jbee.inject.util.Scoped.TARGET_INSTANCE;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import de.jbee.inject.bind.Bootstrapper.ModularBootstrapper;
import de.jbee.inject.util.Provider;
import de.jbee.inject.util.SuppliedBy;

/**
 * Installs all the build-in functionality by using the core API.
 */
public enum BuildinBundle
		implements ModularBundle<BuildinBundle> {
	PROVIDER,
	LIST,
	SET,
	COLLECTION,
	LOGGER;

	@Override
	public void bootstrap( ModularBootstrapper<BuildinBundle> bootstrap ) {
		bootstrap.install( ListBridgeModule.class, LIST );
		bootstrap.install( SetBridgeModule.class, SET );
		bootstrap.install( CollectionBridgeModule.class, COLLECTION );
		bootstrap.install( ProviderBridgeModule.class, PROVIDER );
		bootstrap.install( LoggerModule.class, LOGGER );
	}

	private static class LoggerModule
			extends BinderModule {

		@Override
		protected void declare() {
			per( TARGET_INSTANCE ).starbind( Logger.class ).to( SuppliedBy.LOGGER );
		}

	}

	private static class ProviderBridgeModule
			extends BinderModule {

		@Override
		protected void declare() {
			per( DEPENDENCY_TYPE ).starbind( Provider.class ).to( SuppliedBy.PROVIDER_BRIDGE );
		}

	}

	private static class ListBridgeModule
			extends BinderModule {

		@Override
		protected void declare() {
			per( DEPENDENCY_TYPE ).starbind( List.class ).to( SuppliedBy.LIST_BRIDGE );
		}

	}

	private static class SetBridgeModule
			extends BinderModule {

		@Override
		protected void declare() {
			per( DEPENDENCY_TYPE ).starbind( Set.class ).to( SuppliedBy.SET_BRIDGE );
		}

	}

	private static class CollectionBridgeModule
			extends BinderModule {

		@Override
		protected void declare() {
			asDefault().per( DEPENDENCY_TYPE ).starbind( Collection.class ).toParametrized(
					List.class );
		}
	}

}