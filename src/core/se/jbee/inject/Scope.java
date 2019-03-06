/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static se.jbee.inject.Name.named;

import java.io.File;

/**
 * A {@linkplain Scope} describes a particular lifecycle.
 * 
 * Thereby the {@linkplain Scope} itself acts as a factory for
 * {@link Repository}s. Each {@link Injector} has a single
 * {@linkplain Repository} for each {@linkplain Scope}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
@FunctionalInterface
public interface Scope {

	/**
	 * @param serialID ID number of this {@link InjectionCase} with the
	 *            {@link Injector} context
	 * @param dep currently served {@link Dependency}
	 * @param provider constructor function yielding new instances if needed.
	 *            All {@link Scope}s have to make sure they only ever call
	 *            {@link Provider#provide()} once and only if it is actually
	 *            required and yields the instance used.
	 * @param generators the total number of {@link Generator}s in the
	 *            {@link Injector} context
	 * @return Existing instances are returned, non-existing are received from
	 *         the given {@link Provider} and added to this {@link Repository}
	 *         (forever if it is an application wide singleton or shorter
	 *         depending on the {@link Scope} that created this
	 *         {@link Repository}).
	 * 
	 *         The information from the {@link Dependency} and
	 *         {@link InjectionCase} can be used to lookup existing instances.
	 */
	<T> T yield(int serialID, Dependency<? super T> dep, Provider<T> provider,
			int generators) throws UnresolvableDependency;

	/**
	 * A special virtual {@link Scope} that can be used in binder APIs as
	 * default to declare that the mirror should be used to determine the actual
	 * scope used.
	 */
	Name mirror = named("@mirror");

	/**
	 * Asks the {@link Provider} once per JVM. Once created the instance is
	 * shared across the JVM and exists until JVM is shutdown.
	 */
	Name jvm = named("jvm");

	/**
	 * A special {@link Scope} provided and created by the {@link Injector}
	 * during bootstrapping the other {@link Scope}s. All {@link Scope} are
	 * automatically bound in this scope.
	 */
	Name container = named("container");

	/**
	 * Application singleton {@link Scope}. Once an instance is created within
	 * the {@link Injector} container it exists till the end of the application.
	 * This means the {@link Provider} is asked once per binding.
	 */
	Name application = named("application");

	/**
	 * Often called the 'default' or 'prototype'-scope. Asks the
	 * {@link Provider} once per injection.
	 */
	Name injection = named("injection");

	/**
	 * Asks the {@link Provider} once per thread per binding which is understand
	 * commonly as a usual 'per-thread' singleton.
	 */
	Name thread = named("thread");

	Name dependency = named("dependency");

	Name dependencyType = named("dependency-type");

	Name dependencyInstance = named("dependency-instance");

	Name targetInstance = named("target-instance");

	public static Name disk(File dir) {
		return Name.named("disk:" + dir.getAbsolutePath());
	}

	/**
	 * Often called the 'default' or 'prototype'-scope. Asks the
	 * {@link Provider} once per injection.
	 */
	public static final Scope INJECTION = Scope::injection;

	@SuppressWarnings("unused")
	public static <T> T injection(int serialID, Dependency<? super T> dep,
			Provider<T> provider, int generators)
			throws UnresolvableDependency {
		return provider.provide();
	}
}
