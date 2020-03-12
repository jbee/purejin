/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static java.util.Collections.emptyList;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import se.jbee.inject.config.Env;

/**
 * Knows how to *resolve* an instance for a given {@link Dependency}.
 *
 * The process of resolving might include creation of instances.
 *
 * Once created a {@link Injector} container consists of a fixed set of
 * {@link Resource}s.
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
	 * @throws UnresolvableDependency in case no {@link Resource} is found that
	 *             could serve the requested instance
	 */
	<T> T resolve(Dependency<T> dependency) throws UnresolvableDependency;

	/* Utility methods */

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

	/**
	 * Resolves all instances that are annotated with the given
	 * {@link Annotation} type.
	 * 
	 * Note that this feature requires use of default macros which add
	 * annotation information as plug-ins which is used by the build-in
	 * {@link AnnotatedWith} supplier to assemble the returned list.
	 * 
	 * @since 19.1
	 * 
	 * @param type the {@link Annotation} which the returned instance's types
	 *            are annotated with.
	 * @return a collection (logical set) of instances of types which have an
	 *         {@link Annotation} of the given type.
	 */
	default Collection<?> annotatedWith(Class<? extends Annotation> type) {
		return resolve(Name.ANY,
				raw(AnnotatedWith.class).parametized(type)).annotated();
	}

	default Collection<?> annotatedWith(Class<? extends Annotation> type,
			ElementType target) {
		return resolve(named(target.name()),
				raw(AnnotatedWith.class).parametized(type)).annotated();
	}

	default Collection<?> annotatedWith(Class<? extends Annotation> type,
			ElementType... anyOfTargets) {
		if (anyOfTargets.length == 0)
			return emptyList();
		EnumSet<ElementType> include = EnumSet.of(anyOfTargets[0],
				anyOfTargets);
		if (include.equals(EnumSet.allOf(ElementType.class)))
			return annotatedWith(type);
		Set<Object> matches = new HashSet<>();
		for (ElementType target : include)
			matches.addAll(annotatedWith(type, target));
		return matches;
	}
}
