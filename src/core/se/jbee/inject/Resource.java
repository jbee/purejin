/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Type.raw;

import java.io.Serializable;

/**
 * Describes WHAT can be injected and WHERE it can be injected.
 *
 * It is an {@link Instance} with added information where the bind applies.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Resource<T>
		implements Typed<T>, Qualifying<Resource<?>>, Serializable {

	public static <T> Resource<T> resource(Class<T> type) {
		return new Resource<>(Instance.anyOf(raw(type)));
	}

	public final Instance<T> instance;
	public final Target target;

	public Resource(Instance<T> instance) {
		this(instance, Target.ANY);
	}

	public Resource(Instance<T> instance, Target target) {
		this.instance = instance;
		this.target = target;
	}

	public boolean isMatching(Dependency<? super T> dependency) {
		return isNameCompatibleWith(dependency) // check names first since default goes sorts first but will not match any named
			&& isAvailableFor(dependency)//
			&& isAssignableTo(dependency); // most 'expensive' check so we do it last
	}

	public boolean isCompatibleWith(Dependency<? super T> dependency) {
		return isNameCompatibleWith(dependency) && isAssignableTo(dependency);
	}

	/**
	 * Does the {@link Type} of this a valid argument for the one of the
	 * {@link Dependency} given ?
	 */
	public boolean isAssignableTo(Dependency<? super T> dependency) {
		Type<T> offered = instance.type();
		Type<? super T> required = dependency.type();
		return offered.isAssignableTo(required)
			|| required.rawType.isAssignableFrom(offered.rawType)
				&& offered.hasTypeParameter()
				&& offered.areAllTypeParametersAreUpperBounds();
	}

	/**
	 * Does the given {@link Dependency} occur in the right package and for the
	 * right target ?
	 */
	public boolean isAvailableFor(Dependency<? super T> dependency) {
		return target.isAvailableFor(dependency);
	}

	/**
	 * Does this resource provide the instance wanted by the given
	 * {@link Dependency}'s {@link Name}
	 */
	public boolean isNameCompatibleWith(Dependency<? super T> dependency) {
		return instance.name.isCompatibleWith(dependency.instance.name);
	}

	@Override
	public Type<T> type() {
		return instance.type();
	}

	@Override
	public boolean moreQualiedThan(Resource<?> other) {
		return Qualifying.compareRelated(instance, other.instance, target,
				other.target);
	}

	@Override
	public String toString() {
		return instance + " " + target;
	}

	@Override
	public <E> Resource<E> typed(Type<E> type) {
		return new Resource<>(instance.typed(type), target);
	}

	public boolean equalTo(Resource<?> other) {
		return this == other
			|| instance.equalTo(other.instance) && target.equalTo(other.target);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Resource && equalTo((Resource<?>) obj);
	}

	@Override
	public int hashCode() {
		return instance.hashCode() ^ target.hashCode();
	}

	/**
	 * @since 19.1
	 * @return a {@link Dependency} that {@link #isMatching(Dependency)} this
	 *         {@link Resource}.
	 */
	public Dependency<T> toDependency() {
		Dependency<T> dep = dependency(instance);
		if (target != Target.ANY) {
			if (!target.parents.isAny()) {
				for (Instance<?> p : target.parents)
					dep = dep.injectingInto(p);
			}
			dep = dep.injectingInto(target.instance);
		}
		return dep;
	}
}
