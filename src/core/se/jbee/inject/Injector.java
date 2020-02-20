/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;

import java.lang.reflect.Array;

import se.jbee.inject.UnresolvableDependency.NoCaseForDependency;
import se.jbee.inject.UnresolvableDependency.NoMethodForDependency;

/**
 * Knows how to *resolve* an instance for a given {@link Dependency}.
 *
 * The process of resolving might include creation of instances.
 *
 * Once created a {@link Injector} container consists of a fixed set of
 * {@link InjectionCase}s.
 * 
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
	 *            includes nesting within the resolution process and other
	 *            details of a {@link Dependency} to consider.
	 * @return The resolved instance or
	 * @throws UnresolvableDependency in case no {@link InjectionCase} is found
	 *             that could serve the requested instance
	 */
	<T> T resolve(Dependency<T> dependency) throws UnresolvableDependency;

	/* Utility methods */

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

	/**
	 * Chains {@link Injector}s similar to a {@link ClassLoader} hierarchy.
	 * 
	 * @since 19.1
	 * @param root context acting as fallback
	 * @param branch context that is tried first when resolving dependencies
	 * @return An {@link Injector} with both contexts where root acts as
	 *         fallback
	 */
	static Injector hierarchy(Injector root, Injector branch) {
		return new Injector() {

			@Override
			public <T> T resolve(Dependency<T> dep)
					throws UnresolvableDependency {
				if (dep.type().arrayDimensions() == 1)
					return Injector.resolveArray(dep, root, branch);
				try {
					return branch.resolve(dep);
				} catch (NoCaseForDependency | NoMethodForDependency e) {
					return root.resolve(dep);
				}
			}

		};
	}

	@SuppressWarnings("unchecked")
	static <T> T resolveArray(Dependency<T> dep, Injector root,
			Injector branch) {
		T branchInstance = null;
		T rootInstance = null;
		try {
			branchInstance = branch.resolve(dep);
		} catch (UnresolvableDependency e) {
			return root.resolve(dep);
		}
		try {
			rootInstance = root.resolve(dep);
		} catch (UnresolvableDependency e) {
			return branchInstance;
		}
		int rootLength = Array.getLength(rootInstance);
		int branchLength = Array.getLength(branchInstance);
		Object arr = Array.newInstance(dep.type().baseType().rawType,
				rootLength + branchLength);
		System.arraycopy(rootInstance, 0, arr, 0, rootLength);
		System.arraycopy(branchInstance, 0, arr, rootLength, branchLength);
		return (T) arr;
	}
}
