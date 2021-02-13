/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import se.jbee.lang.Qualifying;
import se.jbee.lang.Type;

import java.util.function.Function;

import static se.jbee.inject.DeclarationType.IMPLICIT;
import static se.jbee.lang.Type.raw;

/**
 * A {@link Resource} describes a injection situation or scenario through its
 * {@link #signature} and {@link #lifeCycle}. If the {@link Resource} applies
 * to a actual {@link Dependency} situation its {@link #generator} is used to
 * create the instance injected should it not exist already.
 *
 * @since 8.1
 *
 * @param <T> type of instances yielded by the {@link #generator}.
 */
public final class Resource<T> implements Comparable<Resource<?>>,
		Qualifying<Resource<?>>, Generator<T> {

	public static <T> Type<Resource<T>> resourceTypeOf(Class<T> type) {
		return resourceTypeOf(raw(type));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> Type<Resource<T>> resourceTypeOf(Type<T> type) {
		return (Type) raw(Resource.class).parameterized(type);
	}

	public static <T> Type<Resource<T>[]> resourcesTypeOf(Class<T> type) {
		return resourcesTypeOf(raw(type));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> Type<Resource<T>[]> resourcesTypeOf(Type<T> type) {
		return (Type) raw(Resource[].class).parameterized(type);
	}

	public final Generator<T> generator;

	/**
	 * The {@link Locator} represented by the {@link Generator} of this info.
	 */
	public final Locator<T> signature;

	/**
	 * The {@link Source} that that defined the binding which corresponds to
	 * this {@link Resource}).
	 */
	public final Source source;

	/**
	 * The information on this {@link Scope} behaviour in relation to other
	 * {@link Scope}s.
	 */
	public final ScopeLifeCycle lifeCycle;

	/**
	 * The serial ID of this {@link Resource}. It is unique within the same
	 * {@link Injector} context and assigned by the {@link Injector}
	 * implementation when it creates the cases during initialisation.
	 */
	public final int serialID;

	/**
	 * Access to the annotations that should be considered in connection with
	 * this {@link Resource}. For example the annotation of the {@link
	 * java.lang.reflect.Constructor} that internally is used to create a
	 * instance.
	 */
	public final Annotated annotations;

	/**
	 * An optional verification check for this {@link Resource} executed at the
	 * end of the bootstrapping phase.
	 */
	public final Verifier verifier;

	public Resource(int serialID, Source source, ScopeLifeCycle lifeCycle,
			Locator<T> signature, Annotated annotations, Verifier verifier,
			Function<Resource<T>, Generator<T>> generator) {
		this.signature = signature;
		this.source = source;
		this.lifeCycle = lifeCycle;
		this.serialID = serialID;
		this.annotations = annotations;
		this.verifier = verifier;
		//OBS! must be last
		this.generator = generator.apply(this);
	}

	private Resource( int serialID,  Source source,
			ScopeLifeCycle lifeCycle, Locator<T> signature, Annotated annotations,
			Verifier verifier, Generator<T> generator) {
		this.generator = generator;
		this.signature = signature;
		this.source = source;
		this.lifeCycle = lifeCycle;
		this.serialID = serialID;
		this.annotations = annotations;
		this.verifier = verifier;
	}

	@Override
	public T generate(Dependency<? super T> dep) throws UnresolvableDependency {
		return generator.generate(dep);
	}

	public T generate() throws UnresolvableDependency {
		return generate(signature.toDependency());
	}

	/**
	 * Called during bootstrapping to initialise eager {@link Resource}s.
	 */
	public void init() {
		if (lifeCycle.isEager())
			generate();
	}

	public Resource<T> withGenerator(Generator<T> generator) {
		return new Resource<>(serialID, source, lifeCycle, signature,
				annotations, verifier, generator);
	}

	public Type<T> type() {
		return signature.type();
	}

	@Override
	public String toString() {
		return "#" + serialID + " " + signature + " " + source + " "
			+ lifeCycle;
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
		Locator<?> a = signature;
		Locator<?> b = other.signature;
		Class<?> aRaw = a.type().rawType;
		Class<?> bRaw = b.type().rawType;
		// first of all we must sort by raw type
		if (aRaw != bRaw) {
			if (aRaw.isAssignableFrom(bRaw))
				return 1;
			if (bRaw.isAssignableFrom(aRaw))
				return -1;
			return aRaw.getName().compareTo(bRaw.getName());
		}
		// secondly any implicit bind is always after any other type of bind
		if (source.declarationType == IMPLICIT && other.source.declarationType != IMPLICIT)
			return 1;
		if (source.declarationType != IMPLICIT && other.source.declarationType == IMPLICIT)
			return -1;
		// for same type and non implicit (or both implicit) binds compare their Locator
		return Qualifying.compare(a, b);
	}

	@Override
	public boolean moreQualifiedThan(Resource<?> other) {
		return signature.moreQualifiedThan(other.signature);
	}

}
