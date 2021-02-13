/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import se.jbee.lang.Type;

import java.lang.annotation.Annotation;
import java.util.List;

import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.lang.Type.raw;

/**
 * Knows how to *resolve* an instance for a given {@link Dependency}.
 * <p>
 * The process of resolving might include creation of instances.
 * <p>
 * Once created a {@link Injector} container consists of a fixed set of {@link
 * Resource}s.
 * <p>
 * Calls to {@link #resolve(Dependency)} always have the same result for the
 * same {@linkplain Dependency}. The only exception to this are scoping effects
 * (expiring and parallel instances).
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
@FunctionalInterface
public interface Injector {

	/**
	 * To resolve all matching implementations create a {@link Dependency} on
	 * the array type of the implementations. When list or set bridges have been
	 * installed this can also be resolved as list or set.
	 *
	 * @param dependency describes the absolute instance to resolve. This
	 *                   includes nesting within the resolution process and
	 *                   other details of a {@link Dependency} to consider.
	 * @return The resolved instance or
	 * @throws UnresolvableDependency in case no {@link Resource} is found that
	 *                                could serve the requested instance
	 */
	<T> T resolve(Dependency<T> dependency) throws UnresolvableDependency;

	/* Utility methods */

	/**
	 * @return This {@link Injector} context as {@link Env} context
	 */
	default Env asEnv() {
		return resolve(Name.AS, Env.class);
	}

	default <T> T resolve(Class<T> type) {
		return resolve(dependency(type));
	}

	default <T> T resolve(Type<T> type) {
		return resolve(dependency(type));
	}

	default <T> T resolve(String name, Class<T> type) {
		return resolve(named(name), type);
	}

	default <T> T resolve(Name name, Class<T> type) {
		return resolve(name, raw(type));
	}

	default <T> T resolve(Name name, Type<T> type) {
		return resolve(instance(name, type));
	}

	default <T> T resolve(Instance<T> inst) {
		return resolve(dependency(inst));
	}

	default Injector subContext(Class<?> target) {
		return subContext(target.getName());
	}

	default Injector subContext(String name) {
		return resolve(name, Injector.class);
	}

	@SuppressWarnings("unchecked")
	default List<AnnotatedWith.AnnotatedInstance<?>> annotatedWith(
			Class<? extends Annotation> annotationType) {
		return resolve(raw(AnnotatedWith.class) //
				.parameterized(annotationType)).instances();
	}

	/**
	 * Listener interface invoked by the {@link Injector}. Implementations are
	 * bound as part of the {@link Injector} context.
	 * <p>
	 * Keep in mind that any instance implementing {@link Observer} is created
	 * ahead of the tracking so these cannot be tracked themselves even if they
	 * qualify as instances in a permanent scope ({@link
	 * ScopeLifeCycle#isPermanent()}).
	 *
	 * @since 8.1
	 */
	@FunctionalInterface
	interface Observer {

		/**
		 * Called by an {@link Injector} context when an instance was created
		 * and after eventual {@link Lift} initialisation have been applied.
		 * <p>
		 * Note that this method is only called for typical "singleton"
		 * instances of an application with a permanent scope ({@link
		 * ScopeLifeCycle#isPermanent()}).
		 *
		 * @param resource the {@link Resource} that is the source of the
		 *                 provided instance
		 * @param instance the created instance to observe by this listener
		 */
		void afterLift(Resource<?> resource, Object instance);

		/**
		 * Observes only instances in the provided scope.
		 *
		 * @param scope The {@link Name} of the {@link Scope} to observe
		 * @return A new {@link Observer} that is limited to observe instances
		 * created in the provided scope
		 */
		default Observer inScope(Name scope) {
			Observer self = this;
			return (resource, instance) -> {
				if (resource.lifeCycle.scope.equalTo(scope))
					self.afterLift(resource, instance);
			};
		}

		/**
		 * Merges a list of {@link Observer}s into a single {@link Observer}
		 *
		 * @param observers a single, multiple or none
		 * @return a merged {@link Observer}, {@code null} in case none were
		 * provided
		 */
		static Observer merge(Observer... observers) {
			if (observers == null || observers.length == 0)
				return null;
			if (observers.length == 1)
				return observers[0];
			return (resource, instance) -> {
				for (Observer observer : observers)
					observer.afterLift(resource, instance);
			};
		}
	}
}
