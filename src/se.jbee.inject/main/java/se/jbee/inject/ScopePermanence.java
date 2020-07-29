/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static se.jbee.inject.Name.named;
import static se.jbee.inject.Utils.arrayAppend;
import static se.jbee.inject.Utils.arrayContains;
import static se.jbee.inject.Utils.arrayEquals;

import java.io.Serializable;

/**
 * A {@link ScopePermanence} is a description for the life-cycle relations
 * between one {@link Scope} and other {@link Scope}s which determines which
 * {@link Scope}s are valid to nest within each other and which are not.
 * 
 * The {@link #scope} can also be made {@link #eager()} to have instances within
 * this {@link Scope} be created during bootstrapping when an {@link Injector}
 * context is created.
 * 
 * @since 19.1
 */
public final class ScopePermanence implements Serializable {

	public static final ScopePermanence ignore = scopePermanence(
			named("@ignore")).stableByNature();
	public static final ScopePermanence reference = scopePermanence(
			Scope.reference).stableByNature();

	public static final ScopePermanence singleton = scopePermanence(
			named("@singleton")).stableByNature();

	public static final ScopePermanence container = singleton.derive(
			Scope.container);

	public static final ScopePermanence unstable = scopePermanence(
			named("@unstable"));

	public static final ScopePermanence disk = scopePermanence(named("@disk"));

	@SafeVarargs
	public static ScopePermanence scopePermanence(Name scope,
			Name... stableInScopes) {
		return new ScopePermanence(scope, stableInScopes, false, false, null);
	}

	public final Name scope;
	private final Name[] stableInScopes;
	private final boolean stableByNature;
	private final boolean eager;
	private final ScopePermanence kind;

	private ScopePermanence(Name scope, Name[] stableInScopes,
			boolean stableByNature, boolean eager, ScopePermanence kind) {
		this.scope = scope;
		this.stableByNature = stableByNature;
		this.stableInScopes = stableInScopes;
		this.eager = eager;
		this.kind = kind;
	}

	public ScopePermanence stableByNature() {
		return new ScopePermanence(scope, stableInScopes, true, eager, kind);
	}

	/**
	 * Declares this {@link ScopePermanence} as being stable in the given
	 * parent.
	 * 
	 * Declares the given parent {@link Scope} at least as stable as this scope.
	 * This means this {@link Scope} can be injected into the given parent
	 * {@link Scope} without wrapping it in a {@link Provider} or alike.
	 * 
	 * @param parent another {@link Scope} type
	 * @return this for chaining
	 */
	public ScopePermanence canBeInjectedInto(Name parent) {
		return new ScopePermanence(scope, arrayAppend(stableInScopes, parent),
				stableByNature, eager, kind);
	}

	public ScopePermanence ofKind(ScopePermanence kind) {
		if (!kind.isKind())
			throw new IllegalArgumentException(
					"Scoping is not a group: " + kind);
		return new ScopePermanence(scope, kind.stableInScopes,
				kind.stableByNature, kind.eager, kind);
	}

	public ScopePermanence derive(Name scope) {
		return scopePermanence(scope).ofKind(this);
	}

	public ScopePermanence eager() {
		return new ScopePermanence(scope, stableInScopes, stableByNature, true,
				kind);
	}

	public ScopePermanence lazy() {
		return new ScopePermanence(scope, stableInScopes, stableByNature, false,
				kind);
	}

	public boolean equalTo(ScopePermanence other) {
		return scope.equalTo(other.scope)
			&& stableByNature == other.stableByNature //
			&& eager == other.eager //
			&& arrayEquals(stableInScopes, other.stableInScopes, Name::equalTo)
			&& (kind == other.kind || kind != null && other.kind != null
				&& kind.equalTo(other.kind));
	}

	public boolean isEager() {
		return eager;
	}

	public boolean isKind() {
		return scope.value.startsWith("@");
	}

	public boolean isStableIn(ScopePermanence parent) {
		return isStableByNature() || parent.isIgnore() || isIgnore()
			|| arrayContains(stableInScopes, s -> s.equalTo(parent.scope))
			|| parent.kind != null && arrayContains(stableInScopes,
					s -> s.equalTo(parent.kind.scope))
			|| (kind != null && kind.isStableIn(parent));
	}

	@Override
	public String toString() {
		return isIgnore() ? "*" : scope.toString() + (eager ? "!" : "");
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ScopePermanence && equalTo((ScopePermanence) obj);
	}

	@Override
	public int hashCode() {
		return scope.hashCode();
	}

	public boolean isIgnore() {
		return this == ignore;
	}

	/**
	 * @return when {@code true} instances with this {@link ScopePermanence} are
	 *         stable, that means once created, they exist throughout the
	 *         life-span of the application (the {@link Injector} context).
	 */
	public boolean isStableByNature() {
		return stableByNature;
	}

}
