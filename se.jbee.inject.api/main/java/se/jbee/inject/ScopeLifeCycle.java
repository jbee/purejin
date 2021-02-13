/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.io.Serializable;

import static se.jbee.inject.Name.named;
import static se.jbee.lang.Utils.*;

/**
 * A {@link ScopeLifeCycle} is an immutable description for the life-cycle
 * relations between one {@link Scope} and other {@link Scope}s which determines
 * which {@link Scope}s are valid to nest within each other and which are not.
 * <p>
 * The referenced {@link #scope} can also be made {@link #eager()} to have
 * instances within this {@link Scope} be created during bootstrapping when an
 * {@link Injector} context is created. By default {@link ScopeLifeCycle} are
 * {@link #lazy()}.
 *
 * To set the used {@link ScopeLifeCycle} bind a named instance.
 *
 * @since 8.1
 */
public final class ScopeLifeCycle implements Serializable {

	/**
	 * Refers to a "virtual" scope where any life-cycle considerations are
	 * turned off. There is no corresponding {@link Scope} as instances are not
	 * bound with this {@link ScopeLifeCycle}. It is only used during processing
	 * to disable evaluation.
	 */
	public static final ScopeLifeCycle ignore = scopeLifeCycle(
			named("@ignore")).permanent();

	/**
	 * See {@link Scope#reference}
	 */
	public static final ScopeLifeCycle reference = scopeLifeCycle(
			Scope.reference).permanent();

	/**
	 * The group of singleton scopes are all scopes that are {@link
	 * #permanent()}. This is used to derive singleton scopes so they all
	 * "inherit" the configuration and play nice with each other.
	 */
	public static final ScopeLifeCycle singleton = scopeLifeCycle(
			named("@singleton")).permanent();

	/**
	 * See {@link Scope#container}
	 */
	public static final ScopeLifeCycle container = singleton.derive(
			Scope.container);

	/**
	 * Is the group of scopes that generally are not stable. That means
	 * instances within this scope can become stale or invalid for some reason
	 * while they are referenced in the application.
	 * <p>
	 * Classic examples are instances that reflect resources that are outside of
	 * the control of the program or JVM, like hardware resources.
	 * <p>
	 * Other members of this group are unstable by choice like the {@link
	 * Scope#injection}.
	 */
	public static final ScopeLifeCycle unstable = scopeLifeCycle(
			named("@unstable"));

	/**
	 * A group of scopes whose instances reflect persistent storage on disk.
	 */
	public static final ScopeLifeCycle disk = scopeLifeCycle(named("@disk"));

	public static ScopeLifeCycle scopeLifeCycle(Name scope,
			Name... consistentInScopes) {
		return new ScopeLifeCycle(scope, consistentInScopes, false, false,
				null);
	}

	/**
	 * {@link Name} reference to the {@link Scope} this {@link ScopeLifeCycle}
	 * describes
	 */
	public final Name scope;
	private final Name[] consistentInScopes;
	private final boolean permanent;
	private final boolean eager;
	private final ScopeLifeCycle group;

	private ScopeLifeCycle(Name scope, Name[] consistentInScopes,
			boolean permanent, boolean eager, ScopeLifeCycle group) {
		this.scope = scope;
		this.permanent = permanent;
		this.consistentInScopes = consistentInScopes;
		this.eager = eager;
		this.group = group;
	}

	public ScopeLifeCycle permanent() {
		return new ScopeLifeCycle(scope, consistentInScopes, true, eager,
				group);
	}

	/**
	 * Declares this {@link ScopeLifeCycle} as being stable and consistent when
	 * used within the given parent.
	 * <p>
	 * Declares the given parent {@link Scope} at least as stable as this scope.
	 * This means this {@link Scope} can be injected into the given parent
	 * {@link Scope} without wrapping it in a {@link Provider} or alike.
	 *
	 * @param parent another {@link Scope} type
	 * @return a new instance with the parent added
	 */
	public ScopeLifeCycle canBeInjectedInto(Name parent) {
		return new ScopeLifeCycle(scope,
				arrayAppend(consistentInScopes, parent), permanent, eager,
				group);
	}

	private ScopeLifeCycle groupedAs(ScopeLifeCycle group) {
		if (!group.isGroup())
			throw new IllegalArgumentException(
					ScopeLifeCycle.class.getSimpleName() + " is not a group: " + group);
		return new ScopeLifeCycle(scope, group.consistentInScopes,
				group.permanent, group.eager, group);
	}

	public ScopeLifeCycle derive(Name scope) {
		return scopeLifeCycle(scope).groupedAs(this);
	}

	public ScopeLifeCycle eager() {
		if (!isPermanent())
			throw new IllegalStateException(
					"Must be permanent to become eager but was " + this);
		return eager
			   ? this
			   : new ScopeLifeCycle(scope, consistentInScopes, true, true,
					   group);
	}

	/**
	 * @return a new {@link ScopeLifeCycle} similar to this except not being
	 * {@link #isEager()}
	 */
	public ScopeLifeCycle lazy() {
		return !eager
			   ? this
			   : new ScopeLifeCycle(scope, consistentInScopes, permanent, false,
					   group);
	}

	public boolean equalTo(ScopeLifeCycle other) {
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

	public boolean isLazy() {
		return !isEager();
	}

	public boolean isGroup() {
		return scope.value.startsWith("@");
	}

	public boolean isConsistentIn(ScopeLifeCycle other) {
		return isPermanent() || other.isIgnored() || isIgnored()
			|| arrayContains(consistentInScopes, s -> s.equalTo(other.scope))
			|| other.group != null && arrayContains(consistentInScopes,
					s -> s.equalTo(other.group.scope))
			|| (group != null && group.isConsistentIn(other));
	}

	@Override
	public String toString() {
		return isIgnored() ? "*" : scope.toString() + (eager ? "!" : "");
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ScopeLifeCycle && equalTo((ScopeLifeCycle) obj);
	}

	@Override
	public int hashCode() {
		return scope.hashCode();
	}

	public boolean isIgnored() {
		return this == ignore;
	}

	/**
	 * @return when {@code true} instances with this {@link ScopeLifeCycle}
	 * exist throughout the life-span of the application (the {@link Injector}
	 * context) wherefore there cannot be issues of stale state with instances
	 * in this scope.
	 */
	public boolean isPermanent() {
		return permanent;
	}

}
