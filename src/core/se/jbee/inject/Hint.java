/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.arrayMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import se.jbee.inject.bootstrap.InjectionSite;

/**
 * A {@link Hint} is a suggested reference for parameters of a
 * {@link Constructor} or {@link Method} to inject.
 * 
 * @since 19.1
 *
 * @param <T> The {@link Type} of the argument
 */
public final class Hint<T> implements Parameter<T> {

	private static final Hint<?>[] NO_PARAMS = new Hint<?>[0];

	/**
	 * A {@link Type} reference is relative to the {@link InjectionSite#site}
	 * hierarchy. It can be pre-resolved for each site.
	 * 
	 * @param target The type to resolve as parameter within the hierarchy
	 * @return A {@link Hint} representing the relative reference
	 */
	public static <T> Hint<T> relativeReferenceTo(Type<T> target) {
		return new Hint<>(target, null, anyOf(target), null);
	}

	/**
	 * An {@link Instance} reference is relative to the
	 * {@link InjectionSite#site} hierarchy. It can be pre-resolved for each
	 * site.
	 * 
	 * @param target The {@link Instance} to resolve as parameter within the
	 *            hierarchy
	 * @return A {@link Hint} representing the relative reference
	 */
	public static <T> Hint<T> relativeReferenceTo(Instance<T> target) {
		return new Hint<>(target.type, null, target, null);
	}

	/**
	 * A {@link Dependency} reference is absolute. That means it ignores the
	 * {@link InjectionSite#site} hierarchy.
	 * 
	 * @param target The {@link Dependency} to resolve as parameter
	 * @return A {@link Hint} representing the absolute reference
	 */
	public static <T> Hint<T> absoluteReferenceTo(Dependency<T> target) {
		return new Hint<>(target.type(), null, target.instance, target);
	}

	public static <T> Hint<T> constant(T constant) {
		if (constant == null)
			throw InconsistentDeclaration.incomprehensiveHint(null);
		@SuppressWarnings("unchecked")
		Type<T> type = (Type<T>) raw(constant.getClass());
		return new Hint<>(type, constant, null, null);
	}

	public static <T> Hint<T> constantNull(Type<T> asType) {
		return new Hint<>(asType, null, null, null);
	}

	@SuppressWarnings("unchecked")
	@SafeVarargs
	public static <E> Hint<? extends E>[] bind(
			Parameter<? extends E>... elems) {
		return arrayMap(elems, Hint.class, Parameter::asHint);
	}

	public static Hint<?>[] bind(Type<?>[] types, Parameter<?>... hints) {
		if (types.length == 0)
			return NO_PARAMS;
		Hint<?>[] args = new Hint<?>[types.length];
		for (Parameter<?> hint : hints) {
			int i = indexForType(types, hint, args);
			if (i < 0)
				throw InconsistentDeclaration.incomprehensiveHint(hint);
			args[i] = hint.asHint();
		}
		for (int i = 0; i < args.length; i++)
			if (args[i] == null)
				args[i] = types[i].asHint();
		return args;
	}

	private static int indexForType(Type<?>[] types, Parameter<?> hint,
			Hint<?>[] args) {
		for (int i = 0; i < types.length; i++)
			if (args[i] == null && hint.type().isAssignableTo(types[i]))
				return i;
		return -1;
	}

	public final T value;
	public final Instance<? extends T> relativeRef;
	public final Dependency<? extends T> absoluteRef;
	public final Type<T> asType;

	public Hint(Type<T> asType, T value, Instance<? extends T> relativeRef,
			Dependency<? extends T> absoluteRef) {
		this.asType = asType;
		this.value = value;
		this.relativeRef = relativeRef;
		this.absoluteRef = absoluteRef;
	}

	@Override
	public Hint<T> asHint() {
		return this;
	}

	@Override
	public Type<T> type() {
		return asType;
	}

	public boolean isConstant() {
		return relativeRef == null && absoluteRef == null;
	}

	@Override
	public <S> Hint<S> asType(Type<S> supertype) {
		return typed(supertype);
	}

	@Override
	public <S> Hint<S> asType(Class<S> supertype) {
		return typed(raw(supertype));
	}

	/**
	 * @param type The new type of this {@link Hint}
	 * @throws ClassCastException In case the given type is incompatible with
	 *             the previous one.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <E> Hint<E> typed(Type<E> type) {
		asType.toSupertype(type);
		return new Hint<>(type, (E) value, (Instance<E>) relativeRef,
				(Dependency<E>) absoluteRef);
	}

	@Override
	public String toString() {
		if (isConstant())
			return "value as " + asType;
		return "ref to " + (absoluteRef != null ? absoluteRef : relativeRef)
			+ " as " + asType;
	}

}
