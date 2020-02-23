/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static se.jbee.inject.Packages.packageAndSubPackagesOf;
import static se.jbee.inject.Packages.packageOf;
import static se.jbee.inject.Packages.subPackagesOf;
import static se.jbee.inject.Type.raw;

import java.io.Serializable;

/**
 * Describes where a {@link Resource} is available for injection.
 *
 * This can be restricted by the {@link Packages} the injected that is injected
 * is defined in or the {@link Type} of the receiving instance.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
@SuppressWarnings("squid:S1448")
public final class Target implements Qualifying<Target>, Serializable {

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
	 * @since 19.1
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

	public boolean isAvailableFor(Dependency<?> dep) {
		return isAccessibleFor(dep) && isCompatibleWith(dep);
	}

	/**
	 * @return true in case the actual types of the injection hierarchy are
	 *         assignable with the ones demanded by this target.
	 */
	public boolean isCompatibleWith(Dependency<?> dep) {
		return areParentsCompatibleWith(dep)
			&& (instance.isAny() || isCompatibleWith(dep.target()));
	}

	private boolean isCompatibleWith(Instance<?> target) {
		return instance.name.isCompatibleWith(target.name)
			&& isAssingableTo(instance.type(), target.type());
	}

	private boolean areParentsCompatibleWith(Dependency<?> dep) {
		if (parents.isAny())
			return true;
		int pl = parents.depth();
		int il = dep.injectionDepth() - 1;
		if (pl > il) {
			return false;
		}
		int pi = 0;
		while (pl <= il && pl > 0) {
			if (isAssingableTo(parents.at(pi).type(), dep.target(il).type())) {
				pl--;
				pi++;
			}
			il--;
		}
		return pl == 0;
	}

	private static boolean isAssingableTo(Type<?> type, Type<?> targetType) {
		return type.isInterface() || type.isAbstract()
			? targetType.isAssignableTo(type)
			: targetType.equalTo(type);
	}

	public boolean isAccessibleFor(Dependency<?> dependency) {
		return packages.contains(dependency.target().type());
	}

	@Override
	public String toString() {
		return "{" + parents + " => "
			+ (instance.isAny() ? "*" : instance.toString()) + ", [" + packages
			+ "] }" + (indirect ? "!" : "");
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
	public boolean moreQualiedThan(Target other) {
		final int ol = other.parents.depth();
		final int l = parents.depth();
		if (ol != l)
			return l > ol;
		if (l > 0) { // length is known to be equal
			if (parents.moreQualiedThan(other.parents))
				return true;
			if (other.parents.moreQualiedThan(parents))
				return false;
		}
		return Qualifying.compareRelated(instance, other.instance, packages,
				other.packages);
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
