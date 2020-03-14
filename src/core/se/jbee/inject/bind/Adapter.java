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

import se.jbee.inject.AnnotatedWith;
import se.jbee.inject.Dependency;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.Provider;
import se.jbee.inject.Scope;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Supply;
import se.jbee.inject.declare.Bindings;
import se.jbee.inject.declare.Bootstrapper.Toggler;
import se.jbee.inject.declare.Bundle;
import se.jbee.inject.declare.Toggled;
import se.jbee.inject.extend.Extension;
import se.jbee.inject.extend.Plugins;

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
	ENV,
	/**
	 * Adds: The {@link DefaultScopes}
	 */
	SCOPES,
	/**
	 * Adds: {@link Extension}s via {@link ExtensionModule}.
	 */
	EXTENSION,
	/**
	 * Adds: {@link AnnotatedWith} via {@link AnnotatedWithModule}.
	 */
	ANNOTATED_WITH,;

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
		bootstrapper.install(DefaultScopes.class, SCOPES);
		bootstrapper.install(ExtensionModule.class, EXTENSION);
		bootstrapper.install(AnnotatedWithModule.class, ANNOTATED_WITH);
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

	private static final class SubContextModule extends BinderModule
			implements se.jbee.inject.container.Supplier<Injector>, Injector {

		@Override
		protected void declare() {
			asDefault().per(Scope.dependencyInstance).starbind(
					Injector.class).toSupplier(this);
		}

		@Override
		public Injector supply(Dependency<? super Injector> dep,
				Injector context) throws UnresolvableDependency {
			@SuppressWarnings("unchecked")
			Class<? extends Bundle>[] bundles = (Class<? extends Bundle>[]) //
			context.resolve(Plugins.class).forPoint(Injector.class,
					dep.instance.name.toString());
			if (bundles.length == 0)
				return this; // this module acts as an Injector that directly fails to resolve any Dependency 
			return Bootstrap.injector(
					context.resolve(Name.DEFAULT, Type.raw(Env.class)),
					Bindings.newBindings(), bundles);
		}

		@Override
		public <T> T resolve(Dependency<T> dep) throws UnresolvableDependency {
			throw new UnresolvableDependency.NoResourceForDependency(
					"Empty SubContext Injector", dep);
		}

	}

	private static final class DefaultEnvModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault().bind(Env.class).to(env());
		}

	}

}