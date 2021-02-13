/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import se.jbee.lang.Qualifying;
import se.jbee.lang.Type;

import java.io.Serializable;

import static se.jbee.inject.Packages.*;
import static se.jbee.lang.Type.raw;

/**
 * Describes where a {@link Locator} is available for injection.
 *
 * This can be restricted by the {@link Packages} the injected that is injected
 * is defined in or the {@link Type} of the receiving instance.
 */
@SuppressWarnings("squid:S1448")
public final class Target
		implements Qualifying<Target>, Serializable, Comparable<Target> {

	public static final Target ANY = targeting(Instance.ANY);

	public static Target targeting(Class<?> type) {
		return targeting(raw(type));
	}

	public static Target targeting(Type<?> type) {
		return targeting(Instance.anyOf(type));
	}

	public static Target targeting(Instance<?> instance) {
		return new Target(Instances.ANY, instance, Packages.ALL, false);
	}

	public final Instances parents;
	public final Instance<?> instance;
	public final Packages packages;
	/**
	 * When true, access to this {@link Target} is only permitted in when it is
	 * used though an interface.
	 */
	public final boolean indirect;

	private Target(Instances parents, Instance<?> instance, Packages packages,
			boolean indirect) {
		this.parents = parents;
		this.instance = instance;
		this.packages = packages;
		this.indirect = indirect;
	}

	/**
	 * @since 8.1
	 *
	 * @return Same as this {@link Target} but also {@link #indirect}
	 */
	public Target indirect() {
		return indirect(true);
	}

	public Target indirect(boolean indirect) {
		return this.indirect == indirect
			? this
			: new Target(parents, instance, packages, indirect);
	}

	public Target within(Instance<?> parent) {
		return new Target(parents.push(parent), instance, packages, indirect);
	}

	public Target injectingInto(Instance<?> instance) {
		return new Target(parents, instance, packages, indirect);
	}

	public Target in(Packages packages) {
		return new Target(parents, instance, packages, indirect);
	}

	public Target injectingInto(Type<?> type) {
		return injectingInto(Instance.anyOf(type));
	}

	public Target injectingInto(Class<?> type) {
		return injectingInto(raw(type));
	}

	/**
	 * @return true if this {@link Target} matches any and all {@link
	 * Dependency}s, in other words it does not filter at all
	 * @since 8.1
	 */
	public boolean isAny() {
		return equalTo(ANY);
	}

	public boolean isUsableFor(Dependency<?> dep) {
		return isAny() || isUsablePackageWise(dep) && isUsableInstanceWise(dep);
	}

	/**
	 * @return true in case the actual types of the injection hierarchy are
	 *         assignable with the ones demanded by this target.
	 */
	public boolean isUsableInstanceWise(Dependency<?> dep) {
		return (instance.isAny() || isUsableInstanceWise(dep.target()))
				&& isUsableParentWise(dep);
	}

	private boolean isUsableInstanceWise(Instance<?> required) {
		return isSuitableInstance(required, this.instance);
	}

	private static boolean isSuitableInstance(Instance<?> required,
			Instance<?> offered) {
		return offered.name.isCompatibleWith(required.name)
			&& isUsableTypeWise(required.type(), offered.type());
	}

	private boolean isUsableParentWise(Dependency<?> dep) {
		if (parents.isAny())
			return true;
		int pl = parents.depth();
		int il = dep.injectionDepth() - 1;
		if (pl > il) {
			return false;
		}
		int pi = 0;
		while (pl <= il && pl > 0) {
			if (isSuitableInstance(parents.at(pi), dep.target(il))) {
				pl--;
				pi++;
			}
			il--;
		}
		return pl == 0;
	}

	private static boolean isUsableTypeWise(Type<?> required, Type<?> offered) {
		return offered.isInterface() || offered.isAbstract()
			? required.isAssignableTo(offered)
			: required.equalTo(offered);
	}

	public boolean isUsablePackageWise(Dependency<?> dep) {
		return this == ANY || packages.contains(dep.target().type());
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		if (indirect)
			str.append("!");
		if (!packages.includesAll())
			str.append(" in package " + packages);
		if (!parents.isAny() || !instance.isAny()) {
			str.append(" when injecting ");
			str.append(instance.isAny() ? "*" : instance.toString());
			if (!parents.isAny())
				str.append( " into " + parents);
		}
		return str.toString();
	}

	public Target inPackageAndSubPackagesOf(Class<?> type) {
		return in(packageAndSubPackagesOf(type));
	}

	public Target inPackageOf(Class<?> type) {
		return in(packageOf(type));
	}

	public Target inSubPackagesOf(Class<?> type) {
		return in(subPackagesOf(type));
	}

	@Override
	public boolean moreQualifiedThan(Target other) {
		final int ol = other.parents.depth();
		final int l = parents.depth();
		if (ol != l)
			return l > ol;
		if (l > 0) { // length is known to be equal
			if (parents.moreQualifiedThan(other.parents))
				return true;
			if (other.parents.moreQualifiedThan(parents))
				return false;
		}
		return Qualifying.compareRelated(instance, other.instance, packages,
				other.packages);
	}

	@Override
	public int compareTo(Target other) {
		int res = instance.compareTo(other.instance);
		if (res != 0)
			return res;
		res = parents.compareTo(other.parents);
		if (res != 0)
			return res;
		return packages.compareTo(other.packages);
	}

	public boolean equalTo(Target other) {
		return this == other || packages.equalTo(other.packages)
			&& instance.equalTo(other.instance)
			&& parents.equalTo(other.parents);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Target && equalTo((Target) obj);
	}

	@Override
	public int hashCode() {
		return instance.hashCode() ^ parents.hashCode();
	}
}
