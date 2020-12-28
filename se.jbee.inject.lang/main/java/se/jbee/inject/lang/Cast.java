/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.lang;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static se.jbee.inject.lang.Type.raw;

/**
 * Utility to get rid of warnings for JRE generic {@link Type}s.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class Cast {

	private Cast() {
		throw new UnsupportedOperationException("util");
	}

	public static <T> Type<List<T>> listTypeOf(Class<T> elementType) {
		return listTypeOf(raw(elementType));
	}

	public static <T> Type<List<T>> listTypeOf(Type<T> elementType) {
		return (Type) raw(List.class).parameterized(elementType);
	}

	public static <T> Type<Set<T>> setTypeOf(Class<T> elementType) {
		return setTypeOf(raw(elementType));
	}

	public static <T> Type<Set<T>> setTypeOf(Type<T> elementType) {
		return (Type) raw(Set.class).parameterized(elementType);
	}

	public static <T> Type<Collection<T>> collectionTypeOf(
			Class<T> elementType) {
		return collectionTypeOf(raw(elementType));
	}

	public static <T> Type<Collection<T>> collectionTypeOf(
			Type<T> elementType) {
		return (Type) raw(Collection.class).parameterized(elementType);
	}

	public static <A, B> Type<Function<A,B>> functionTypeOf(Class<A> a, Class<B> b) {
		return functionTypeOf(raw(a), raw(b));
	}

	public static <A, B> Type<Function<A,B>> functionTypeOf(Type<A> a, Type<B> b) {
		return (Type) Type.raw(Function.class).parameterized(a, b);
	}
}
