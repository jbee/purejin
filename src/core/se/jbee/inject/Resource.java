/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.util.function.Function;

/**
 * A {@link Resource} describes a injection situation or scenario through its
 * {@link #locator} and {@link #scoping}. If the {@link Resource} applies to a
 * actual {@link Dependency} situation its {@link #generator} is used to create
 * the instance injected should it not exist already.
 * 
 * @since 19.1
 * 
 * @param <T> type of instances yielded by the {@link #generator}.
 */
public final class Resource<T> implements Comparable<Resource<?>>,
		Qualifying<Resource<?>>, Generator<T> {

	public final Generator<T> generator;

	/**
	 * The {@link Locator} represented by the {@link Generator} of this info.
	 */
	public final Locator<T> locator;

	/**
	 * The {@link Source} that {@link Injection} had been created from (e.g. did
	 * define the bind).
	 */
	public final Source source;

	/**
	 * The information on this {@link Scope} behaviour in relation to other
	 * {@link Scope}s.
	 */
	public final Scoping scoping;

	/**
	 * The serial ID of this {@link Resource}. It is unique within the same
	 * {@link Injector} context and assigned by the {@link Injector}
	 * implementation when it creates the cases during initialisation.
	 */
	public final int serialID;

	public Resource(int serialID, Source source, Scoping scoping,
			Locator<T> locator, Function<Resource<T>, Generator<T>> generator) {
		this.locator = locator;
		this.source = source;
		this.scoping = scoping;
		this.serialID = serialID;
		//OBS! must be last
		this.generator = generator.apply(this);
	}

	@Override
	public T generate(Dependency<? super T> dep) throws UnresolvableDependency {
		return generator.generate(dep);
	}

	public Type<T> type() {
		return locator.type();
	}

	@Override
	public String toString() {
		return "#" + serialID + " " + locator + " " + source + " " + scoping;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Resource
			&& ((Resource<?>) obj).serialID == serialID;
	}

	@Override
	public int hashCode() {
		return serialID;
	}

	@Override
	public int compareTo(Resource<?> other) {
		Locator<?> l1 = locator;
		Locator<?> l2 = other.locator;
		Class<?> c1 = l1.type().rawType;
		Class<?> c2 = l2.type().rawType;
		if (c1 != c2) {
			if (c1.isAssignableFrom(c2))
				return 1;
			if (c2.isAssignableFrom(c1))
				return -1;
			return c1.getName().compareTo(c2.getName());
		}
		return Qualifying.compare(l1, l2);
	}

	@Override
	public boolean moreQualiedThan(Resource<?> other) {
		return locator.moreQualiedThan(other.locator);
	}

}
