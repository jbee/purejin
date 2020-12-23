/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.Typed;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.lang.Type.parameterTypes;
import static se.jbee.inject.lang.Type.raw;

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
		return new Hint<>(target, null, anyOf(target), null);
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
		return new Hint<>(target.type, null, target, null);
	}

	/**
	 * A {@link Dependency} reference is absolute. That means it ignores the
	 * injection site hierarchy.
	 *
	 * @param target The {@link Dependency} to resolve as parameter
	 * @return A {@link Hint} representing the absolute reference
	 */
	public static <T> Hint<T> absoluteReferenceTo(Dependency<T> target) {
		return new Hint<>(target.type(), null, target.instance, target);
	}

	public static <T> Hint<T> constant(T constant) {
		if (constant == null)
			throw InconsistentDeclaration.incomprehensibleHint(null);
		@SuppressWarnings("unchecked")
		Type<T> type = (Type<T>) raw(constant.getClass());
		return new Hint<>(type, constant, null, null);
	}

	public static <T> Hint<T> constantNull(Type<T> asType) {
		return new Hint<>(asType, null, null, null);
	}

	public static boolean matchesInOrder(Executable member, Hint<?>[] hints) {
		if (hints.length == 0)
			return true;
		Type<?>[] types = parameterTypes(member);
		int i = 0;
		for (Hint<?> hint : hints) {
			while (i < types.length && !hint.asType.isAssignableTo(types[i]))
				i++;
			if (i >= types.length)
				return false;
		}
		return true;
	}

	public static boolean matchesInRandomOrder(Executable member, Hint<?>[] hints) {
		if (hints.length == 0)
			return true;
		Type<?>[] types = parameterTypes(member);
		for (Hint<?> hint : hints) {
			boolean matched = false;
			int i = 0;
			while (!matched && i < types.length) {
				Type<?> type = types[i];
				if (type != null && hint.asType.isAssignableTo(type)) {
					types[i] = null; // nark as handled by removing it
					matched = true;
				}
				i++;
			}
			if (!matched)
				return false;
		}
		return true;
	}

	public static Hint<?>[] match(Type<?>[] types, Hint<?>... hints) {
		if (types.length == 0)
			return NO_PARAMS;
		Hint<?>[] args = new Hint<?>[types.length];
		for (Hint<?> hint : hints) {
			int i = indexForType(types, hint, args);
			if (i < 0)
				throw InconsistentDeclaration.incomprehensibleHint(hint);
			args[i] = hint.parameterized(types[i]);
		}
		for (int i = 0; i < args.length; i++)
			if (args[i] == null)
				args[i] = Hint.relativeReferenceTo(types[i]);
		return args;
	}

	private static int indexForType(Type<?>[] types, Hint<?> hint,
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

	public Hint(Type<T> asType, T value, Instance<? extends T> relativeRef,
			Dependency<? extends T> absoluteRef) {
		this.asType = asType;
		this.value = value;
		this.relativeRef = relativeRef;
		this.absoluteRef = absoluteRef;
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
	private Hint<?> parameterized(Type<?> type) {
		return type.moreQualifiedThan(asType) ? typed(type).asType(asType) : this;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public <S> Hint<S> asType(Type<S> supertype) {
		asType.toSupertype(supertype); // throws if this is not legal
		return new Hint<>((Type) supertype, value, relativeRef, absoluteRef);
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
		return new Hint<>(type, (E) value, newRelRef, newAbsRef);
	}

	@Override
	public String toString() {
		if (isConstant())
			return "value as " + asType;
		return "ref to " + (absoluteRef != null ? absoluteRef : relativeRef)
			+ " as " + asType;
	}

	public Hint<?> withActualType(java.lang.reflect.Parameter param,
			Map<java.lang.reflect.TypeVariable<?>, Type<?>> actualTypeArguments) {
		if (value != null || absoluteRef != null)
			return this;
		java.lang.reflect.Type genericType = param.getParameterizedType();
		Type<?> actualType = Type.genericType(genericType, actualTypeArguments);
		if (param.getType() == Type.class) {
			return constant(actualType.parameter(0));
		}
		return relativeReferenceTo(instance(relativeRef.name, actualType));
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
