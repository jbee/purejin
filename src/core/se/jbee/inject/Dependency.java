/*
 *  Copyright (c) 2012-2019, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static java.util.Arrays.asList;
import static se.jbee.inject.Instance.defaultInstanceOf;
import static se.jbee.inject.Name.pluginFor;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.arrayAppend;
import static se.jbee.inject.Utils.arrayContains;
import static se.jbee.inject.Utils.arrayDropTail;
import static se.jbee.inject.Utils.arrayEquals;
import static se.jbee.inject.Utils.arrayFindFirst;
import static se.jbee.inject.Utils.arrayMap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

import se.jbee.inject.UnresolvableDependency.DependencyCycle;
import se.jbee.inject.UnresolvableDependency.UnstableDependency;

/**
 * Describes what is wanted/needed as parameter to construct a instance of T.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Dependency<T>
		implements Parameter<T>, Iterable<Injection>, Serializable {

	/**
	 * A empty {@link Injection} hierarchy. It is used whenever the
	 * {@link Dependency} does not depend on the actual hierarchy. This is the
	 * default.
	 */
	private static final Injection[] UNTARGETED = new Injection[0];

	public static Dependency<Class<?>[]> pluginsFor(Class<?> pluginPoint) {
		return pluginsFor(pluginPoint, Name.ANY.toString());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Dependency<Class<?>[]> pluginsFor(Class<?> pluginPoint,
			String property) {
		return (Dependency) dependency(raw(Class[].class)).named(
				pluginFor(pluginPoint, property));
	}

	public static <T> Dependency<T> dependency(Class<T> type) {
		return dependency(raw(type));
	}

	public static <T> Dependency<T> dependency(Type<T> type) {
		return dependency(type, UNTARGETED);
	}

	public static <T> Dependency<T> dependency(Instance<T> instance) {
		return dependency(instance, UNTARGETED);
	}

	private static <T> Dependency<T> dependency(Type<T> type,
			Injection[] hierarchy) {
		return dependency(Instance.instance(Name.ANY, type), hierarchy);
	}

	private static <T> Dependency<T> dependency(Instance<T> instance,
			Injection[] hierarchy) {
		return new Dependency<>(instance, hierarchy);
	}

	private final Injection[] hierarchy;
	public final Instance<T> instance;

	private Dependency(Instance<T> instance, Injection... hierarchy) {
		this.instance = instance;
		this.hierarchy = hierarchy;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Dependency && equalTo((Dependency<?>) obj);
	}

	@Override
	public int hashCode() {
		return instance.hashCode() ^ Arrays.hashCode(hierarchy);
	}

	public boolean equalTo(Dependency<?> other) {
		// cheapest first...
		return instance.equalTo(other.instance)
			&& arrayEquals(hierarchy, other.hierarchy, Injection::equalTo);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		for (Injection i : hierarchy)
			b.append(i.target).append(" -> ");
		b.append(instance);
		return b.toString();
	}

	@Override
	public Type<T> type() {
		return instance.type;
	}

	@Override
	public <E> Dependency<E> typed(Type<E> type) {
		return instanced(instance.typed(type));
	}

	public Dependency<?> onTypeParameter() {
		return typed(type().parameter(0));
	}

	public Dependency<T> named(String name) {
		return named(Name.named(name));
	}

	public Dependency<T> named(Name name) {
		return instanced(instance.named(name));
	}

	public <E> Dependency<E> instanced(Instance<E> instance) {
		return dependency(instance, hierarchy);
	}

	public Dependency<T> untargeted() {
		return dependency(instance, UNTARGETED);
	}

	public Dependency<T> ignoredScoping() {
		return hierarchy.length == 0
			? this
			: dependency(instance,
					arrayMap(hierarchy, Injection::ignoredScoping));
	}

	public boolean isUntargeted() {
		return hierarchy.length == 0;
	}

	public Instance<?> target() {
		return target(0);
	}

	public Instance<?> target(int level) {
		return level >= hierarchy.length
			? Instance.ANY
			: hierarchy[hierarchy.length - 1 - level].target.instance;
	}

	public int injectionDepth() {
		return hierarchy.length;
	}

	/**
	 * Means we inject into the argument target class.
	 */
	public Dependency<T> injectingInto(Class<?> target)
			throws DependencyCycle, UnstableDependency {
		return injectingInto(raw(target));
	}

	public Dependency<T> injectingInto(Type<?> target)
			throws DependencyCycle, UnstableDependency {
		return injectingInto(defaultInstanceOf(target));
	}

	public <I> Dependency<T> injectingInto(Instance<I> target)
			throws DependencyCycle, UnstableDependency {
		return injectingInto(new Resource<>(target), Scoping.ignore);
	}

	public Dependency<T> injectingInto(Resource<?> target, Scoping scoping)
			throws DependencyCycle, UnstableDependency {
		Injection injection = new Injection(instance, target, scoping);
		if (hierarchy.length == 0)
			return new Dependency<>(instance, injection);
		ensureStableScopeNesting(injection);
		ensureNoDependencyCycle(injection);
		return new Dependency<>(instance, arrayAppend(hierarchy, injection));
	}

	public Dependency<T> uninject() {
		return hierarchy.length <= 1
			? untargeted()
			: new Dependency<>(instance, arrayDropTail(hierarchy, 1));
	}

	private void ensureNoDependencyCycle(Injection injection)
			throws DependencyCycle {
		if (arrayContains(hierarchy, injection, Injection::equalTo))
			throw new DependencyCycle(this, injection.target);
	}

	private void ensureStableScopeNesting(Injection injection)
			throws UnstableDependency {
		Injection unstable = arrayFindFirst(hierarchy,
				e -> !injection.scoping.isStableIn(e.scoping));
		if (unstable != null)
			throw new UnstableDependency(unstable, injection);
	}

	@Override
	public Iterator<Injection> iterator() {
		return asList(hierarchy).iterator();
	}
}