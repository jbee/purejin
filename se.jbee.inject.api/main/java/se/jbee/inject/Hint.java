/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import se.jbee.lang.Type;
import se.jbee.lang.Typed;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Instance.instance;
import static se.jbee.lang.Type.raw;

/**
 * A {@link Hint} is a suggested reference for parameters of a
 * {@link Constructor} or {@link Method} to inject.
 *
 * @since 8.1
 *
 * @param <T> The {@link Type} of the argument
 */
public final class Hint<T> implements Typed<T>, Descriptor {

	private static final Hint<?>[] NO_PARAMS = new Hint<?>[0];

	public static Hint<?>[] none() {
		return NO_PARAMS;
	}

	public static Hint<?>[] signature(Class<?> ... parameterTypes) {
		if (parameterTypes.length == 0)
			return none();
		return Arrays.stream(parameterTypes).map(
				Hint::relativeReferenceTo).toArray(Hint[]::new);
	}

	public static <T> Hint<T> relativeReferenceTo(Class<T> target) {
		return relativeReferenceTo(raw(target));
	}

	/**
	 * A {@link Type} reference is relative to the injection site
	 * hierarchy. It can be pre-resolved for each site.
	 *
	 * @param target The type to resolve as parameter within the hierarchy
	 * @return A {@link Hint} representing the relative reference
	 */
	public static <T> Hint<T> relativeReferenceTo(Type<T> target) {
		return new Hint<>(target, null, anyOf(target), null, null);
	}

	/**
	 * An {@link Instance} reference is relative to the
	 * injection site hierarchy. It can be pre-resolved for each
	 * site.
	 *
	 * @param target The {@link Instance} to resolve as parameter within the
	 *            hierarchy
	 * @return A {@link Hint} representing the relative reference
	 */
	public static <T> Hint<T> relativeReferenceTo(Instance<T> target) {
		return new Hint<>(target.type, null, target, null, null);
	}

	/**
	 * A {@link Dependency} reference is absolute. That means it ignores the
	 * injection site hierarchy.
	 *
	 * @param target The {@link Dependency} to resolve as parameter
	 * @return A {@link Hint} representing the absolute reference
	 */
	public static <T> Hint<T> absoluteReferenceTo(Dependency<T> target) {
		return new Hint<>(target.type(), null, target.instance, target, null);
	}

	public static <T> Hint<T> constant(T constant) {
		if (constant == null)
			throw InconsistentDeclaration.incomprehensibleHint(null);
		@SuppressWarnings("unchecked")
		Type<T> type = (Type<T>) raw(constant.getClass());
		return new Hint<>(type, constant, null, null, null);
	}

	public static <T> Hint<T> constantNull(Type<T> asType) {
		return new Hint<>(asType, null, null, null, null);
	}

	public static int indexForType(Type<?>[] types, Hint<?> hint,
			Hint<?>[] args) {
		Type<?> target = hint.type();
		// 1. lowest index with exact type match, or else
		for (int i = 0; i < types.length; i++)
			if (args[i] == null && target.equalTo(types[i]))
				return i;
		// 2. lowest index with same raw type and assignable, or else
		for (int i = 0; i < types.length; i++)
			if (args[i] == null && target.rawType == types[i].rawType //
					&& target.isAssignableTo(types[i]))
				return i;
		// 3. lowest index with assignable type, or else
		for (int i = 0; i < types.length; i++)
			if (args[i] == null && target.isAssignableTo(types[i]))
				return i;
		// 4. not found
		return -1;
	}

	public final T value;
	public final Instance<? extends T> relativeRef;
	public final Dependency<? extends T> absoluteRef;
	public final Type<T> asType;
	public final InjectionPoint at;

	private Hint(Type<T> asType, T value, Instance<? extends T> relativeRef,
			Dependency<? extends T> absoluteRef, InjectionPoint at) {
		this.asType = asType;
		this.value = value;
		this.relativeRef = relativeRef;
		this.absoluteRef = absoluteRef;
		this.at = at;
	}

	public boolean isAtInjectionPoint() {
		return at != null;
	}

	@Override
	public Type<T> type() {
		return asType;
	}

	public boolean isConstant() {
		return relativeRef == null && absoluteRef == null;
	}

	/**
	 * @return a more qualified {@link Hint} if the provided type is more
	 * qualified which changes the references but does not change {@link #asType}
	 * in case that was intentionally set to a supertype.
	 *
	 * At the point this method is called the {@link #asType} already had its
	 * effect but we still want to preserve the information for debugging so
	 * we do not get confused.
	 */
	public Hint<?> parameterized(Type<?> type) {
		return type.moreQualifiedThan(asType) ? typed(type).asType(asType) : this;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public <S> Hint<S> asType(Type<S> supertype) {
		asType.castTo(supertype); // throws if this is not legal
		return new Hint<>((Type) supertype, value, relativeRef, absoluteRef, at);
	}

	public Hint<T> at(InjectionPoint point) {
		return at == point ? this : new Hint<>(asType, value, relativeRef, absoluteRef, point);
	}

	public <S> Hint<S> asType(Class<S> supertype) {
		return asType(raw(supertype));
	}

	/**
	 * @param type The new type of this {@link Hint}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <E> Hint<E> typed(Type<E> type) {
		Instance<? extends E> newRelRef = relativeRef == null
				? null
				: relativeRef.typed(type);
		Dependency<? extends E> newAbsRef = absoluteRef == null
				? null
				: absoluteRef.typed(type);
		return new Hint<>(type, (E) value, newRelRef, newAbsRef, at);
	}

	@Override
	public String toString() {
		if (isConstant())
			return "value as " + asType;
		return "ref to " + (absoluteRef != null ? absoluteRef : relativeRef)
			+ " as " + asType;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Hint))
			return false;
		Hint<?> other = (Hint<?>) obj;
		if (isConstant())
			return value == other.value;
		if (!asType.equalTo(other.asType))
			return false;
		if (relativeRef != null)
			return other.relativeRef != null && relativeRef.equalTo(other.relativeRef);
		return other.absoluteRef != null && absoluteRef.equalTo(other.absoluteRef);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	public Hint<?> withActualType(java.lang.reflect.Parameter param,
			Map<java.lang.reflect.TypeVariable<?>, Type<?>> actualTypeArguments) {
		if (value != null || absoluteRef != null)
			return this;
		java.lang.reflect.Type genericType = param.getParameterizedType();
		Type<?> actualType = Type.genericType(genericType, actualTypeArguments);
		if (param.getType() == Type.class) {
			return constant(actualType.parameter(0)).at(at);
		}
		return relativeReferenceTo(instance(relativeRef.name, actualType)).at(at);
	}

	@SuppressWarnings("unchecked")
	public T resolveIn(Injector context) {
		if (asType.rawType == Injector.class)
			return (T) context;
		if (isConstant())
			return value;
		if (absoluteRef != null)
			return context.resolve(absoluteRef);
		// relative ref
		return context.resolve(relativeRef);
	}
}
