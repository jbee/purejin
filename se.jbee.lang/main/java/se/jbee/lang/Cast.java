/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.lang;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static se.jbee.lang.Type.raw;

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

	public static <A, B, C> Type<BiFunction<A, B, C>> biFunctionTypeOf(
			Class<A> a, Class<B> b, Class<C> c) {
		return biFunctionTypeOf(raw(a), raw(b), raw(c));
	}

	public static <A, B, C> Type<BiFunction<A, B, C>> biFunctionTypeOf(
			Type<A> a, Type<B> b, Type<C> c) {
		return (Type) Type.raw(BiFunction.class).parameterized(a, b, c);
	}

	public static <T> Type<Consumer<T>> consumerTypeOf(Class<T> consumedType) {
		return consumerTypeOf(raw(consumedType));
	}

	public static <T> Type<Consumer<T>> consumerTypeOf(Type<T> consumedType) {
		return (Type) Type.raw(Consumer.class).parameterized(consumedType);
	}
}
