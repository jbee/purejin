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

import se.jbee.inject.Dependency;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.Provider;
import se.jbee.inject.Scope;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Supply;
import se.jbee.inject.declare.Bootstrapper.Toggler;
import se.jbee.inject.extend.Plugins;
import se.jbee.inject.declare.Bundle;
import se.jbee.inject.declare.Toggled;

/**
 * Installs all the build-in functionality by using the core API.
 */
public enum Adapter implements Toggled<Adapter> {
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
	OPTIONAL,
	/**
	 * Adds: Support for {@link Injector} sub-contexts.
	 */
	SUB_CONTEXT,
	/**
	 * Adds: Binds the bootstrapping {@link Env} as the {@link Name#DEFAULT}
	 * {@link Env} in the {@link Injector} context.
	 */
	ENV;

	@Override
	public void bootstrap(Toggler<Adapter> bootstrapper) {
		bootstrapper.install(ListBridgeModule.class, LIST);
		bootstrapper.install(SetBridgeModule.class, SET);
		bootstrapper.install(CollectionBridgeModule.class, COLLECTION);
		bootstrapper.install(ProviderBridgeModule.class, PROVIDER);
		bootstrapper.install(LoggerModule.class, LOGGER);
		bootstrapper.install(OptionalBridgeModule.class, OPTIONAL);
		bootstrapper.install(SubContextModule.class, SUB_CONTEXT);
		bootstrapper.install(DefaultEnvModule.class, ENV);
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

	private static final class SubContextModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault().per(Scope.dependencyInstance).starbind(
					Injector.class).toSupplier(SubContextModule::lazyInjector);
		}

		final static Injector lazyInjector(Dependency<? super Injector> dep,
				Injector context) {
			@SuppressWarnings("unchecked")
			Class<? extends Bundle>[] bundles = (Class<? extends Bundle>[]) //
			context.resolve(Plugins.class).forPoint(Injector.class,
					dep.instance.name.toString());
			//TODO eventually forward the env here? get from context...
			return Bootstrap.injector(bundles);
		}

	}

	private static final class DefaultEnvModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault().bind(Env.class).to(env());
		}

	}

}