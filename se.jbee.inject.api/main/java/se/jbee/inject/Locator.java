/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import se.jbee.inject.lang.Qualifying;
import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.Typed;

import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.lang.Type.raw;

import java.io.Serializable;

/**
 * Describes WHAT (type-wise) can be injected and WHERE it can be injected.
 *
 * It is an {@link Instance} with added information where the bind applies.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Locator<T> implements Typed<T>, Qualifying<Locator<?>>,
		Serializable, Comparable<Locator<?>> {

	public static <T> Locator<T> locator(Class<T> type) {
		return new Locator<>(Instance.anyOf(raw(type)));
	}

	public final Instance<T> instance;
	public final Target target;

	public Locator(Instance<T> instance) {
		this(instance, Target.ANY);
	}

	public Locator(Instance<T> instance, Target target) {
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
		if (offered.isAssignableTo(required))
			return true;
		if (!required.rawType.isAssignableFrom(offered.rawType)
			|| !offered.hasTypeParameter())
			return false;
		Type<?>[] offeredParams = offered.parameters();
		for (int i = 0; i < offeredParams.length; i++) {
			Type<?> offeredParam = offeredParams[i];
			Type<?> requiredParam = required.parameter(i);
			if (offeredParam.isUpperBound()) {
				if (!offeredParam.isAssignableTo(requiredParam))
					return false;
			} else if (!offeredParam.asParameterAssignableTo(requiredParam)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Does the given {@link Dependency} occur in the right package and for the
	 * right target ?
	 */
	public boolean isAvailableFor(Dependency<? super T> dependency) {
		return target.isAvailableFor(dependency);
	}

	/**
	 * Does this {@link Locator} provide the instance wanted by the given
	 * {@link Dependency}'s {@link Name}
	 */
	public boolean isNameCompatibleWith(Dependency<? super T> dependency) {
		return instance.name.isCompatibleWith(dependency.instance.name);
	}

	public Locator<T> indirect(boolean indirect) {
		return target.indirect == indirect
			? this
			: new Locator<>(instance, target.indirect(indirect));
	}

	@Override
	public Type<T> type() {
		return instance.type();
	}

	@Override
	public boolean moreQualifiedThan(Locator<?> other) {
		return Qualifying.compareRelated(instance, other.instance, target,
				other.target);
	}

	@Override
	public int compareTo(Locator<?> other) {
		int res = instance.compareTo(other.instance);
		if (res != 0)
			return res;
		return target.compareTo(other.target);
	}

	@Override
	public String toString() {
		return instance + "" + target;
	}

	@Override
	public <E> Locator<E> typed(Type<E> type) {
		return new Locator<>(instance.typed(type), target);
	}

	public boolean equalTo(Locator<?> other) {
		return this == other
			|| instance.equalTo(other.instance) && target.equalTo(other.target);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Locator && equalTo((Locator<?>) obj);
	}

	@Override
	public int hashCode() {
		return instance.hashCode() ^ target.hashCode();
	}

	/**
	 * @since 8.1
	 * @return a {@link Dependency} that {@link #isMatching(Dependency)} this
	 *         {@link Locator}.
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
