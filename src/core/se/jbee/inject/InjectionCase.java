/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * A {@link InjectionCase} describes a injection situation or scenario through
 * its {@link #resource} and {@link #scoping}. If the {@link InjectionCase}
 * applies to a actual {@link Dependency} situation its {@link #generator} is
 * used to create the instance injected should it not exist already.
 * 
 * @since 19.1
 * 
 * @param <T> type of instances yielded by the {@link #generator}.
 */
public final class InjectionCase<T>
		implements Comparable<InjectionCase<?>>, Qualifying<InjectionCase<?>> {

	public final Generator<T> generator;

	/**
	 * The {@link Resource} represented by the {@link Generator} of this info.
	 */
	public final Resource<T> resource;

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
	 * The serial ID of this {@link InjectionCase}. It is unique within the same
	 * {@link Injector} context and assigned by the {@link Injector}
	 * implementation when it creates the cases during initialisation.
	 */
	public final int serialID;

	public InjectionCase(int serialID, Source source, Scoping scoping,
			Resource<T> resource, Generator<T> generator) {
		this.generator = generator;
		this.resource = resource;
		this.source = source;
		this.scoping = scoping;
		this.serialID = serialID;
	}

	public Type<T> type() {
		return resource.type();
	}

	@Override
	public String toString() {
		return "#" + serialID + " " + resource + " " + source + " " + scoping;
	}

	@Override
	public int compareTo(InjectionCase<?> other) {
		Resource<?> r1 = resource;
		Resource<?> r2 = other.resource;
		Class<?> c1 = r1.type().rawType;
		Class<?> c2 = r2.type().rawType;
		if (c1 != c2) {
			if (c1.isAssignableFrom(c2))
				return 1;
			if (c2.isAssignableFrom(c1))
				return -1;
			return c1.getCanonicalName().compareTo(c2.getCanonicalName());
		}
		return Qualifying.compare(r1, r2);
	}

	@Override
	public boolean moreQualiedThan(InjectionCase<?> other) {
		return resource.moreQualiedThan(other.resource);
	}
}
