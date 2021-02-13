/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import se.jbee.lang.Type;

import java.io.File;

import static se.jbee.inject.Name.named;

/**
 * A {@linkplain Scope} implements a particular life-cycle behaviour.
 * <p>
 * The {@linkplain Scope} itself acts as a repository of instances. Each {@link
 * Injector} has a single instance of each {@linkplain Scope} used.
 * <p>
 * All {@link Scope} instances themselves are bound as container instances in
 * the {@link #container} scope which is a special "singleton" scope reserved
 * for constants that belong to the {@link Injector} context itself.
 * <p>
 * To set a {@link Scope}s implementation bind a named instance.
 */
@FunctionalInterface
public interface Scope {

	/**
	 * @param serialID  ID number of this {@link Resource} with the {@link
	 *                  Injector} context
	 * @param resources the total number of {@link Resource}s in the {@link
	 *                  Injector} context. This is given to allow optimized
	 *                  organization of instances within the registry
	 *                  implementation of a {@link Scope}.
	 * @param dep       currently served {@link Dependency}
	 * @param provider  constructor function yielding new instances if needed.
	 *                  All {@link Scope}s have to make sure they only ever call
	 *                  {@link Provider#provide()} once and only if it is
	 *                  actually required and yields the instance used.
	 * @return Existing instances are returned, non-existing are received from
	 * the given {@link Provider} and added to this {@link Scope} data structure
	 * (forever if it is an application wide singleton or shorter depending on
	 * the {@link Scope}).
	 * <p>
	 * The information from the {@link Dependency} and {@link Resource} can be
	 * used to lookup existing instances.
	 */
	<T> T provide(int serialID, int resources, Dependency<? super T> dep,
			Provider<T> provider) throws UnresolvableDependency;

	/**
	 * A virtual scope used by the scope configuration {@code ScopedBy } to
	 * indicate that no particular scope should be used. This falls back on
	 * {@link #application}.
	 */
	Name auto = named("@auto");

	/**
	 * A special virtual {@link Scope} that can be used in binder APIs as
	 * default to declare that the mirror should be used to determine the actual
	 * scope used.
	 */
	Name mirror = named("@mirror");

	/**
	 * A special virtual {@link Scope} that is used to bypass actual use of
	 * {@link Scope} abstraction for type references. In such cases the
	 * effective scope is determined by the referenced {@link Resource}.
	 */
	Name reference = named("@ref");

	/**
	 * Asks the {@link Provider} once per JVM. Once created the instance is
	 * shared across the JVM and exists until JVM is shutdown.
	 */
	Name jvm = named("jvm");

	/**
	 * A special {@link Scope} provided and created by the {@link Injector}
	 * during bootstrapping the other {@link Scope}s. All {@link Scope}
	 * instances are automatically bound in this scope.
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

	/**
	 * Is a family of temporary scopes that are explicitly linked and unlinked
	 * with a thread for a period of time. This is usually used for worker
	 * threads that are linked to a fresh scope for each work item. In contrast
	 * to a {@link #thread} scope the worker scope resets when it is unlinked
	 * and linked again for the same {@link Thread}.
	 * <p>
	 * A typical example of a worker scope is a 'per-request' scope in a HTTP
	 * server.
	 */
	Name worker = named("@worker");

	/**
	 * A scope that stores one instance per exact dependency. That means it
	 * considers the {@link Instance} and the {@link Injection} hierarchy of the
	 * {@link Dependency}. An dependency with the same {@link Name} and {@link
	 * Type} within different "parent" or "grandparent" will correspond to a
	 * different instance in this scope.
	 * <p>
	 * This is like a {@code Map<Dependency<?>, Object>} where the key is the
	 * resolved {@link Dependency}.
	 */
	Name dependency = named("dependency");

	/**
	 * A scope that stores one instance per exact {@link Type} that is
	 * resolved/injected. None of the other data attached to the injected {@link
	 * Dependency} is considered.
	 * <p>
	 * This is like a {@code Map<Type<?>, Object>} where the key {@link Type}
	 * depends on the resolved {@link Dependency#type()}.
	 */
	Name dependencyType = named("dependency-type");

	/**
	 * A scope that stores one instance per exact {@link Name} and {@link Type}
	 * that is resolved or injected.
	 * <p>
	 * This is like a {@code Map<Instance<?>, Object>} where the key {@link
	 * Instance} depends on the resolved {@link Dependency#instance}.
	 */
	Name dependencyInstance = named("dependency-instance");

	/**
	 * A scope that stores one instance per exact injection target instance
	 * (that means {@link Name} and {@link Type} of the instance receiving the
	 * resolved {@link Dependency} are considered).
	 * <p>
	 * A classic example would be injection of a "per-class" logger. The
	 * resolved dependency is of the logger type but we want a logger that is
	 * specific to the instance that receives the logger.
	 */
	Name targetInstance = named("target-instance");

	/**
	 * Returns the name of a disk scope with the given directory as its root.
	 *
	 * @param dir the root directory of the disk scope
	 * @return the name to use when binding the {@link Scope} (and {@link
	 * ScopeLifeCycle}).
	 */
	static Name disk(File dir) {
		return Name.named(dir.getAbsolutePath()).in("disk");
	}

	/**
	 * Often called the 'default' or 'prototype'-scope. Asks the {@link
	 * Provider} once per injection. In other words it is a stateless scope that
	 * acts as if there wasn't a concept of scopes and the {@link Provider}
	 * (which represents the underlying {@link Supplier}) would be asked
	 * directly.
	 */
	Scope INJECTION = Scope::injection;

	@SuppressWarnings("unused")
	static <T> T injection(int serialID, int resources,
			Dependency<? super T> dep, Provider<T> provider)
			throws UnresolvableDependency {
		return provider.provide();
	}

	/**
	 * SPI for temporary {@link Thread} bound {@link Scope}s.
	 *
	 * For example to {@link #allocate()} and {@link #deallocate()} the
	 * {@link Scope#worker} to a worker {@link Thread}.
	 *
	 * An instance of the {@link Controller} is resolved from the
	 * {@link Injector} using the {@link Scope}'s {@link Name} as
	 * {@link Instance} name for the {@link Controller} {@link Dependency}.
	 *
	 * To create a fresh {@link Scope} context resolve the instance with the
	 * worker {@link Thread}.
	 *
	 * To transfer an existing {@link Scope} context to a worker {@link Thread}
	 * resolve the instance with the {@link Thread} that should be the source of
	 * the transfer. The source must be {@link #allocate()}ed already. A
	 * transfer is used to e.g. perform asynchronous computation as part of a
	 * work item where the asynchronous computation should have access to
	 * resources of the outer work item.
	 *
	 * This interface is usually implemented by binding the {@link Controller}
	 * in the {@link Scope} it controls so that its implementation can return
	 * the controller implementation.
	 *
	 * @since 8.1
	 */
	interface Controller {

		/**
		 * Activates a worker {@link Scope} for the current {@link Thread}.
		 *
		 * This method must be called by the worker {@link Thread} at the
		 * beginning of starting a work item.
		 *
		 * When this {@link Controller} was resolved within the worker
		 * {@link Thread} a fresh empty {@link Scope} context is created.
		 *
		 * When this {@link Controller} was resolved by another {@link Thread}
		 * the existing {@link Scope} context of that {@link Thread} is
		 * transferred to or shared with the current worker {@link Thread}.
		 * Should the {@link Scope} be {@link #deallocate()}ed by the source of
		 * the transfer it will continue to exist and be valid to use by this
		 * worker {@link Thread} until it is also deallocated by it.
		 *
		 * Such transfer of {@link Scope} from one {@link Thread} to another
		 * worker can occur whether or not the source created the {@link Scope}
		 * or received it by transfer itself.
		 */
		void allocate();

		/**
		 * Deactivates an existing {@link Scope} for the current {@link Thread}.
		 *
		 * This has no effect on the allocation state of other {@link Thread}s,
		 * neither those receiving a transfer from the current {@link Thread}
		 * nor those that transferred to the current worker {@link Thread}.
		 *
		 * This method must be called by the worker {@link Thread} at the end of
		 * working a work item.
		 *
		 * If no {@link Scope} context was allocated for the current
		 * {@link Thread} the call has no effect.
		 */
		void deallocate();

		/**
		 * @param scope a "worker" type scope name
		 * @return the {@link Instance} to bind to when binding the {@link
		 * Controller} for the provided scope
		 */
		static Instance<Controller> forScope(Name scope) {
			return Instance.instance(scope, Type.raw(Controller.class));
		}
	}
}
