/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static se.jbee.inject.Name.named;
import static se.jbee.inject.Utils.arrayAppend;
import static se.jbee.inject.Utils.arrayContains;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The relation of a {@link Scope} to other {@link Scope}s is captured by a
 * {@link Scoping} instance for each {@link Scope}.
 * 
 * @since 19.1
 */
public final class Scoping implements Serializable {

	public static final Scoping ignore = new Scoping(named("@ignore"));
	public static final Scoping singleton = new Scoping(named("@singleton"));
	public static final Scoping disk = new Scoping(named("@disk"));

	private static final Map<Name, Scoping> SCOPING_BY_SCOPE = new ConcurrentHashMap<>();

	//TODO the Scoping context should become non static and something that is bootstrapped with the container
	static {
		ignore.stableByDesign();
		singleton.stableByDesign();
		scopingOf(Scope.container).group(singleton);
		scopingOf(Scope.application).group(singleton);
		scopingOf(Scope.dependency).group(singleton);
		scopingOf(Scope.dependencyType).group(singleton);
		scopingOf(Scope.dependencyInstance).group(singleton);
		scopingOf(Scope.targetInstance).group(singleton);
		scopingOf(Scope.thread) //
				.canBeInjectedInto(Scope.thread)//
				.canBeInjectedInto(Scope.worker) //
				.canBeInjectedInto(Scope.injection); //
		scopingOf(Scope.injection) //
				.canBeInjectedInto(Scope.injection);
		scopingOf(Scope.worker) //
				.canBeInjectedInto(Scope.worker) //
				.canBeInjectedInto(Scope.injection); //
	}

	public static Scoping scopingOf(Name scope) {
		return SCOPING_BY_SCOPE.computeIfAbsent(scope, Scoping::new);
	}

	public final Name scope;
	private boolean stableByDesign;
	private Name[] stableInScopes;
	private boolean eager = false;
	private Scoping group;

	@SafeVarargs
	private Scoping(Name scope, Name... stableInScopes) {
		this.stableByDesign = false;
		this.scope = scope;
		this.stableInScopes = stableInScopes;
		if (scope.value.indexOf(':') > 0)
			group = scopingOf(named(
					"@" + scope.value.substring(0, scope.value.indexOf(':'))));
	}

	public Scoping stableByDesign() {
		stableByDesign = true;
		return this;
	}

	/**
	 * Declares this {@link Scoping} as being stable in the given parent.
	 * 
	 * Declares the given parent {@link Scope} at least as stable as this scope.
	 * This means this {@link Scope} can be injected into the given parent
	 * {@link Scope} without wrapping it in a {@link Provider} or alike.
	 * 
	 * @param parent another {@link Scope} type
	 * @return this for chaining
	 */
	public Scoping canBeInjectedInto(Name parent) {
		stableInScopes = arrayAppend(stableInScopes, parent);
		return this;
	}

	/**
	 * @see #canBeInjectedInto(Name)
	 */
	public Scoping canBeInjectedInto(Scoping parent) {
		return canBeInjectedInto(parent.scope);
	}

	public Scoping group(Name group) {
		return group(Scoping.scopingOf(group));
	}

	public Scoping group(Scoping group) {
		if (!group.isGroup())
			throw new IllegalArgumentException(
					"Scoping is not a group: " + group);
		this.group = group;
		this.stableByDesign = group.stableByDesign;
		this.eager = group.eager;
		this.stableInScopes = group.stableInScopes;
		return this;
	}

	public Scoping eager() {
		eager = true;
		return this;
	}

	public Scoping lazy() {
		eager = false;
		return this;
	}

	public boolean equalTo(Scoping other) {
		return scope.equalTo(other.scope);
	}

	public boolean isEager() {
		return eager;
	}

	public boolean isGroup() {
		return scope.value.startsWith("@");
	}

	public boolean isStableIn(Name scopeOfParent) {
		return isStableIn(scopingOf(scopeOfParent));
	}

	public boolean isStableIn(Scoping parent) {
		return isStableByDesign() || parent.isIgnore() || isIgnore()
			|| arrayContains(stableInScopes, s -> s.equalTo(parent.scope))
			|| parent.group != null && arrayContains(stableInScopes,
					s -> s.equalTo(parent.group.scope))
			|| (group != null && group.isStableIn(parent));
	}

	@Override
	public String toString() {
		return isIgnore() ? "*" : scope.toString();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Scoping && equalTo((Scoping) obj);
	}

	@Override
	public int hashCode() {
		return scope.hashCode();
	}

	public boolean isIgnore() {
		return this == ignore;
	}

	/**
	 * @return when {@code true} instances with this {@link Scoping} are stable,
	 *         that means once created, they exist throughout the life-span of
	 *         the application (the {@link Injector} context).
	 */
	public boolean isStableByDesign() {
		return stableByDesign;
	}

}
