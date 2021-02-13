/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import se.jbee.lang.Qualifying;
import se.jbee.lang.Type;
import se.jbee.lang.Typed;

import java.io.Serializable;

import static se.jbee.lang.Type.raw;

/**
 * Used to tell that we don#t want just one singleton at a time but multiple
 * distinguished by the {@link Name} used.
 */
public final class Instance<T>
		implements Typed<T>, Descriptor, Qualifying<Instance<?>>, Serializable,
		Comparable<Instance<?>> {

	/**
	 * When a wildcard-type is used as bound instance type the bind will be
	 * added to all concrete binds of matching types. There is also a set of
	 * wildcard binds that are tried if no bind has been made for a type.
	 */
	public static final Instance<?> ANY = anyOf(Type.WILDCARD);

	public static <T> Instance<T> defaultInstanceOf(Type<T> type) {
		return instance(Name.DEFAULT, type);
	}

	public static <T> Instance<T> anyOf(Class<T> type) {
		return anyOf(raw(type));
	}

	public static <T> Instance<T> anyOf(Type<T> type) {
		return instance(Name.ANY, type);
	}

	public static <T> Instance<T> instance(Name name, Type<T> type) {
		return new Instance<>(name, type);
	}

	public final Name name;
	public final Type<T> type;

	private Instance(Name name, Type<T> type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Instance && equalTo((Instance<?>) obj);
	}

	@Override
	public int hashCode() {
		return name.hashCode() ^ type.hashCode();
	}

	public boolean equalTo(Instance<?> other) {
		return type.equalTo(other.type) && name.equals(other.name);
	}

	@Override
	public Type<T> type() {
		return type;
	}

	@Override
	public <E> Instance<E> typed(Type<E> type) {
		return new Instance<>(name, type);
	}

	public Instance<T> named(Name name) {
		return new Instance<>(name, type);
	}

	@Override
	public String toString() {
		return type.toString() + (name.isDefault() ? "" : " " + name);
	}

	public boolean isAny() {
		return name.isAny() && type.equalTo(ANY.type);
	}

	@Override
	public boolean moreQualifiedThan(Instance<?> other) {
		return Qualifying.compareRelated(type, other.type, name, other.name);
	}

	@Override
	public int compareTo(Instance<?> other) {
		int res = name.compareTo(other.name);
		if (res != 0)
			return res;
		return type.compareTo(other.type);
	}

	public Hint<T> asHint() {
		return Hint.relativeReferenceTo(this);
	}
}
