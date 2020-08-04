/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.util.function.Function;

/**
 * A {@link Resource} describes a injection situation or scenario through its
 * {@link #signature} and {@link #permanence}. If the {@link Resource} applies
 * to a actual {@link Dependency} situation its {@link #generator} is used to
 * create the instance injected should it not exist already.
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
	public final Locator<T> signature;

	/**
	 * The {@link Source} that {@link Injection} had been created from (e.g. did
	 * define the bind).
	 */
	public final Source source;

	/**
	 * The information on this {@link Scope} behaviour in relation to other
	 * {@link Scope}s.
	 */
	public final ScopePermanence permanence;

	/**
	 * The serial ID of this {@link Resource}. It is unique within the same
	 * {@link Injector} context and assigned by the {@link Injector}
	 * implementation when it creates the cases during initialisation.
	 */
	public final int serialID;

	public final Annotated annotations;

	public Resource(int serialID, Source source, ScopePermanence permanence,
			Locator<T> signature, Function<Resource<T>, Generator<T>> generator,
			Annotated annotations) {
		this.signature = signature;
		this.source = source;
		this.permanence = permanence;
		this.serialID = serialID;
		this.annotations = annotations;
		//OBS! must be last
		this.generator = generator.apply(this);
	}

	@Override
	public T generate(Dependency<? super T> dep) throws UnresolvableDependency {
		return generator.generate(dep);
	}

	public T generate() throws UnresolvableDependency {
		return generate(signature.toDependency());
	}

	public void init() {
		if (permanence.isEager())
			generate();
	}

	public Type<T> type() {
		return signature.type();
	}

	@Override
	public String toString() {
		return "#" + serialID + " " + signature + " " + source + " "
			+ permanence;
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
		Locator<?> l1 = signature;
		Locator<?> l2 = other.signature;
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
	public boolean moreQualifiedThan(Resource<?> other) {
		return signature.moreQualifiedThan(other.signature);
	}

}
