/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import se.jbee.inject.Provider;
import se.jbee.inject.Scope;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.bootstrap.Bootstrapper.ChoiceBootstrapper;
import se.jbee.inject.bootstrap.ChoiceBundle;
import se.jbee.inject.bootstrap.Supply;

/**
 * Installs all the build-in functionality by using the core API.
 */
public enum BuildinBundle implements ChoiceBundle<BuildinBundle> {
	/**
	 * Adds: {@link Provider}s can be injected for all bound types.
	 */
	PROVIDER,
	/**
	 * Adds: {@link List}s can be injected for all bound types (via array
	 * bridge)
	 */
	LIST,
	/**
	 * Adds: {@link Set} can be injected for all bound types (via array bridge)
	 */
	SET,
	/**
	 * Adds: {@link Collection} can be injected instead of {@link List} (needs
	 * explicit List bind).
	 */
	COLLECTION,
	/**
	 * Adds: {@link Logger}s can be injected per receiving class.
	 */
	LOGGER,
	/**
	 * Adds: Support for injection of {@link Optional}s. If {@link Optional}
	 * parameters cannot be resolved {@link Optional#empty()} is injected.
	 */
	OPTIONAL;

	@Override
	public void bootstrap(ChoiceBootstrapper<BuildinBundle> bootstrapper) {
		bootstrapper.install(ListBridgeModule.class, LIST);
		bootstrapper.install(SetBridgeModule.class, SET);
		bootstrapper.install(CollectionBridgeModule.class, COLLECTION);
		bootstrapper.install(ProviderBridgeModule.class, PROVIDER);
		bootstrapper.install(LoggerModule.class, LOGGER);
		bootstrapper.install(OptionalBridgeModule.class, OPTIONAL);
	}

	private static class LoggerModule extends BinderModule {

		@Override
		protected void declare() {
			per(Scope.targetInstance).starbind(Logger.class).toSupplier(
					Supply.LOGGER);
		}

	}

	private static class ProviderBridgeModule extends BinderModule {

		@Override
		protected void declare() {
			per(Scope.dependency).starbind(Provider.class).toSupplier(
					Supply.PROVIDER);
		}

	}

	private static class ListBridgeModule extends BinderModule {

		@Override
		protected void declare() {
			per(Scope.dependency).starbind(List.class).toSupplier(
					Supply.LIST_BRIDGE);
		}

	}

	private static class SetBridgeModule extends BinderModule {

		@Override
		protected void declare() {
			per(Scope.dependency).starbind(Set.class).toSupplier(
					Supply.SET_BRIDGE);
		}

	}

	private static class CollectionBridgeModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault().per(Scope.dependency).starbind(
					Collection.class).toParametrized(List.class);
		}
	}

	private static class OptionalBridgeModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault().per(Scope.dependency).starbind(
					Optional.class).toSupplier((dep, context) -> {
						try {
							return Optional.ofNullable(
									context.resolve(dep.onTypeParameter()));
						} catch (UnresolvableDependency e) {
							return Optional.empty();
						}
					});
		}

	}

}