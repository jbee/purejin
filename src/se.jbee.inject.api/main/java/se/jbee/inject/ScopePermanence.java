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
			named("@ignore")).permanent();
	public static final ScopePermanence reference = scopePermanence(
			Scope.reference).permanent();

	public static final ScopePermanence singleton = scopePermanence(
			named("@singleton")).permanent();

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
	private final Name[] consistentInScopes;
	private final boolean permanent;
	private final boolean eager;
	private final ScopePermanence group;

	private ScopePermanence(Name scope, Name[] consistentInScopes,
			boolean permanent, boolean eager, ScopePermanence group) {
		this.scope = scope;
		this.permanent = permanent;
		this.consistentInScopes = consistentInScopes;
		this.eager = eager;
		this.group = group;
	}

	public ScopePermanence permanent() {
		return new ScopePermanence(scope, consistentInScopes, true, eager,
				group);
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
		return new ScopePermanence(scope,
				arrayAppend(consistentInScopes, parent), permanent, eager,
				group);
	}

	private ScopePermanence groupedAs(ScopePermanence group) {
		if (!group.isGroup())
			throw new IllegalArgumentException(
					"Scoping is not a group: " + group);
		return new ScopePermanence(scope, group.consistentInScopes,
				group.permanent, group.eager, group);
	}

	public ScopePermanence derive(Name scope) {
		return scopePermanence(scope).groupedAs(this);
	}

	public ScopePermanence eager() {
		if (!isPermanent())
			throw new IllegalStateException(
					"Must be permanent to become eager but was " + this);
		return new ScopePermanence(scope, consistentInScopes, true, true,
				group);
	}

	public ScopePermanence lazy() {
		return new ScopePermanence(scope, consistentInScopes, permanent, false,
				group);
	}

	public boolean equalTo(ScopePermanence other) {
		return scope.equalTo(other.scope) && permanent == other.permanent //
			&& eager == other.eager //
			&& arrayEquals(consistentInScopes, other.consistentInScopes,
					Name::equalTo)
			&& (group == other.group || group != null && other.group != null
				&& group.equalTo(other.group));
	}

	public boolean isEager() {
		return eager;
	}

	public boolean isGroup() {
		return scope.value.startsWith("@");
	}

	public boolean isConsistentIn(ScopePermanence other) {
		return isPermanent() || other.isIgnore() || isIgnore()
			|| arrayContains(consistentInScopes, s -> s.equalTo(other.scope))
			|| other.group != null && arrayContains(consistentInScopes,
					s -> s.equalTo(other.group.scope))
			|| (group != null && group.isConsistentIn(other));
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
	public boolean isPermanent() {
		return permanent;
	}

}
