/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static se.jbee.inject.Type.raw;

import java.io.Serializable;

/**
 * Used to tell that we don#t want just one singleton at a time but multiple
 * distinguished by the {@link Name} used.
 *
 * @author Jan Bernitt (jan@jbee.se)
 *
 */
public final class Instance<T>
		implements Parameter<T>, Qualifying<Instance<?>>, Serializable {

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

	public Instance<T> discriminableBy(Name name) {
		return new Instance<>(name, type);
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
		return (name + " " + type).trim();
	}

	public boolean isAny() {
		return name.isAny() && type.equalTo(ANY.type);
	}

	@Override
	public boolean moreQualiedThan(Instance<?> other) {
		return Qualifying.compareRelated(type, other.type,
				name, other.name);
	}

}
