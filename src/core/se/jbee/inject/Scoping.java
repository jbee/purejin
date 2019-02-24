/*
 *  Copyright (c) 2012-2017, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static se.jbee.inject.Array.append;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import se.jbee.inject.Scope.SingletonScope;

/**
 * The relation of a {@link Scope} to other {@link Scope}s is captured by a
 * {@link Scoping} instance for each {@link Scope}.
 * 
 * @serial 19.1
 */
public final class Scoping implements Serializable {

	public static final Scoping IGNORE = new Scoping(SingletonScope.class);
	private static final Map<Class<? extends Scope>, Scoping> EXPIRY_BY_SCOPE = new ConcurrentHashMap<>();

	public static Scoping scopingOf(Scope s) {
		return scopingOf(s.getClass());
	}

	public static Scoping scopingOf(Class<? extends Scope> s) {
		return EXPIRY_BY_SCOPE.computeIfAbsent(s, k -> new Scoping(k));
	}

	private final boolean stableByDesign;
	private final Class<? extends Scope> scope;
	private Class<? extends Scope>[] unstableInScopes;

	@SafeVarargs
	private Scoping(Class<? extends Scope> scope,
			Class<? extends Scope>... unstableInScopes) {
		this.stableByDesign = SingletonScope.class.isAssignableFrom(scope);
		this.scope = scope;
		this.unstableInScopes = unstableInScopes;
	}

	/**
	 * Declares the given parent {@link Scope} as less stable as this scope.
	 * This means this {@link Scope} cannot be injected into the given parent
	 * {@link Scope}.
	 * 
	 * @see #notStableIn(Class)
	 * 
	 * @param parent another {@link Scope}
	 * @return this for chaining
	 */
	public Scoping notStableIn(Scope parent) {
		return notStableIn(parent.getClass());
	}

	/**
	 * Declares the given parent {@link Scope} as less stable as this scope.
	 * This means this {@link Scope} cannot be injected into the given parent
	 * {@link Scope}.
	 * 
	 * @param parent another {@link Scope} type
	 * @return this for chaining
	 */
	public Scoping notStableIn(Class<? extends Scope> parent) {
		unstableInScopes = append(unstableInScopes, parent);
		return this;
	}

	public boolean equalTo(Scoping other) {
		return scope == other.scope;
	}

	public boolean isStableIn(Scope parent) {
		return isStableIn(scopingOf(parent));
	}

	public boolean isStableIn(Scoping parent) {
		if (isStable())
			return true;
		for (int i = 0; i < unstableInScopes.length; i++)
			if (unstableInScopes[i] == parent.scope)
				return false;
		return true;
	}

	public boolean isStable() {
		return unstableInScopes == null || unstableInScopes.length == 0;
	}

	@Override
	public String toString() {
		return isIgnore()
			? "*"
			: String.valueOf(scope.getSimpleName().replaceAll("Scope", ""));
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
		return scope == SingletonScope.class;
	}

	/**
	 * @return {@code true} in case the {@link Scope} represented implements the
	 *         {@link Scope.SingletonScope} interface which is a marker for
	 *         scopes that create instances that, once created, exist throughout
	 *         the life-span of the application.
	 */
	public boolean isStableByDesign() {
		return stableByDesign;
	}

}
