/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import se.jbee.inject.UnresolvableDependency.DependencyCycle;
import se.jbee.inject.UnresolvableDependency.UnstableDependency;
import se.jbee.lang.Type;
import se.jbee.lang.Typed;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

import static java.util.Arrays.asList;
import static se.jbee.inject.Instance.defaultInstanceOf;
import static se.jbee.lang.Type.raw;
import static se.jbee.lang.Utils.*;

/**
 * Describes what is wanted/needed as parameter to construct a instance of T.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
@SuppressWarnings("squid:S1448")
public final class Dependency<T>
		implements Typed<T>, Iterable<Injection>, Serializable {

	/**
	 * A empty {@link Injection} hierarchy. It is used whenever the
	 * {@link Dependency} does not depend on the actual hierarchy. This is the
	 * default.
	 */
	private static final Injection[] NO_TARGET = new Injection[0];

	public static <T> Dependency<T> dependency(Class<T> type) {
		return dependency(raw(type));
	}

	public static <T> Dependency<T> dependency(Type<T> type) {
		return dependency(Instance.instance(Name.ANY, type), NO_TARGET);
	}

	public static <T> Dependency<T> dependency(Instance<T> instance) {
		return dependency(instance, NO_TARGET);
	}

	private static <T> Dependency<T> dependency(Instance<T> instance,
			Injection[] hierarchy) {
		return new Dependency<>(null, instance, hierarchy);
	}

	private final transient InjectionPoint at;
	private final Injection[] hierarchy;
	public final Instance<T> instance;

	private Dependency(InjectionPoint at, Instance<T> instance, Injection... hierarchy) {
		this.at = at;
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
		b.append(instance);
		for (int i = hierarchy.length - 1; i >= 0; i--)
			b.append(" <= ").append(hierarchy[i].target);
		return b.toString();
	}

	@Override
	public Type<T> type() {
		return instance.type;
	}

	@Override
	public <E> Dependency<E> typed(Type<E> type) {
		return onInstance(instance.typed(type));
	}

	public Dependency<T> at(InjectionPoint point) {
		return at == point ? this : new Dependency<>(point, instance, hierarchy);
	}

	public InjectionPoint at() {
		return at == null ? InjectionPoint.NONE : at;
	}

	public Dependency<?> onTypeParameter() {
		return typed(type().parameter(0));
	}

	public Dependency<T> named(String name) {
		return named(Name.named(name));
	}

	public Dependency<T> named(Name name) {
		return onInstance(instance.named(name));
	}

	public <E> Dependency<E> onInstance(Instance<E> instance) {
		return dependency(instance, hierarchy);
	}

	public Dependency<T> simple() {
		return dependency(instance, NO_TARGET);
	}

	public Dependency<T> ignoredScoping() {
		return hierarchy.length == 0
			? this
			: dependency(instance,
					arrayMap(hierarchy, Injection::ignoredScoping));
	}

	public boolean isNotTargeted() {
		return hierarchy.length == 0;
	}

	public Instance<?> target() {
		return target(0);
	}

	public Instance<?> target(int level) {
		return level >= hierarchy.length
			? Instance.ANY
			: injection(level).target.instance;
	}

	public Injection injection(int level) {
		return hierarchy[hierarchy.length - 1 - level];
	}

	public int injectionDepth() {
		return hierarchy.length;
	}

	/**
	 * @param target Means we inject into the argument target class.
	 * @return a new {@link Dependency} similar to this with the given target
	 *         {@link Class} added to the top of its {@link #hierarchy}
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
		return injectingInto(new Locator<>(target), ScopeLifeCycle.ignore);
	}

	public Dependency<T> injectingInto(Locator<?> target,
			ScopeLifeCycle lifeCycle)
			throws DependencyCycle, UnstableDependency {
		Injection injection = new Injection(instance, target, lifeCycle);
		if (hierarchy.length == 0)
			return new Dependency<>(at, instance, injection);
		ensureStableScopeNesting(injection);
		ensureNoDependencyCycle(injection);
		return new Dependency<>(at, instance, arrayAppend(hierarchy, injection));
	}

	public Dependency<T> uninject() {
		return hierarchy.length <= 1
			? simple()
			: new Dependency<>(at, instance, arrayDropTail(hierarchy, 1));
	}

	private void ensureNoDependencyCycle(Injection injection)
			throws DependencyCycle {
		if (arrayContains(hierarchy, injection, Injection::equalTo))
			throw new DependencyCycle(this, injection.target);
	}

	private void ensureStableScopeNesting(Injection injection)
			throws UnstableDependency {
		Injection unstable = arrayFindFirst(hierarchy,
				e -> !injection.lifeCycle.isConsistentIn(e.lifeCycle));
		if (unstable != null)
			throw new UnstableDependency(unstable, injection);
	}

	public void ensureNoIllegalDirectAccessOf(Locator<? extends T> locator) {
		if (!locator.target.indirect)
			return;
		Type<? super T> required = instance.type;
		if (required.rawType.isInterface())
			return;
		for (int level = 0; level < injectionDepth(); level++) {
			Instance<?> parent = target(level);
			if (!required.isAssignableTo(parent.type()))
				throw new UnresolvableDependency.IllegalAccess(locator, this);
			if (parent.type.rawType.isInterface())
				return;
		}
		throw new UnresolvableDependency.IllegalAccess(locator, this);
	}

	@Override
	public Iterator<Injection> iterator() {
		return asList(hierarchy).iterator();
	}

	public Hint<T> asHint() {
		return Hint.absoluteReferenceTo(this);
	}
}
