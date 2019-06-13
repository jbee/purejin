/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.arrayMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import se.jbee.inject.Dependency;
import se.jbee.inject.InconsistentBinding;
import se.jbee.inject.InjectionCase;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Parameter;
import se.jbee.inject.Scope;
import se.jbee.inject.Type;
import se.jbee.inject.container.Supplier;

/**
 * A {@link Argument} is a {@link Supplier} for parameters of
 * {@link Constructor} or {@link Method} invocations.
 *
 * @author Jan Bernitt (jan@jbee.se)
 *
 * @param <T> The {@link Type} of the argument
 */
public final class Argument<T> implements Parameter<T> {

	@Deprecated // eval other fields to determine, add boolan flag if hierarchy should be used
	public enum ParameterResolution {
		/**
		 * The value is a provided constant that is not necessarily bound in the
		 * {@link Injector} context.
		 */
		NEVER,
		/**
		 * The value is dynamically resolved within the {@link Injector}
		 * context. It represents a fixed {@link InjectionCase} whose value
		 * might be cached in case the {@link Scope} allows it.
		 */
		SIMPLE,
		/**
		 * The value is dynamically resolved within the {@link Injector}
		 * context. There is no fixed {@link InjectionCase} because the value
		 * depends on the injection hierarchy of the currently resolved
		 * dependency.
		 */
		HIERARCHICAL
	}

	private static final Argument<?>[] NO_PARAMS = new Argument<?>[0];

	public static <T> Argument<T> bind(Parameter<T> parameter) {
		if (parameter instanceof Argument<?>) {
			return (Argument<T>) parameter;
		}
		if (parameter instanceof Instance<?>) {
			Instance<T> i = (Instance<T>) parameter;
			return new Argument<>(ParameterResolution.SIMPLE, i.type(), i, null,
					Supply.instance(i));
		}
		if (parameter instanceof Type<?>) {
			Instance<T> i = anyOf((Type<T>) parameter);
			return new Argument<>(ParameterResolution.SIMPLE, i.type(), i, null,
					Supply.instance(i));
		}
		if (parameter instanceof Dependency<?>) {
			final Dependency<T> d = (Dependency<T>) parameter;
			return new Argument<>(ParameterResolution.HIERARCHICAL, d.type(),
					d.instance, null, Supply.dependency(d));
		}
		throw InconsistentBinding.notSupported(parameter);
	}

	public static <T> Parameter<T> constant(Class<T> type, T constant) {
		return constant(raw(type), constant);
	}

	public static <T> Parameter<T> constant(Type<T> type, T constant) {
		return new Argument<>(ParameterResolution.NEVER, type,
				Instance.defaultInstanceOf(type), constant,
				Supply.constant(constant));
	}

	public static <T> Parameter<T> supplier(Type<T> type,
			Supplier<? extends T> supplier) {
		return new Argument<>(ParameterResolution.HIERARCHICAL, type,
				Instance.defaultInstanceOf(type), null, supplier);
	}

	public static <S, T extends S> Parameter<S> asType(Class<S> supertype,
			Parameter<T> parameter) {
		return asType(raw(supertype), parameter);
	}

	public static <S, T extends S> Parameter<S> asType(Type<S> supertype,
			Parameter<T> parameter) {
		return bind(parameter).typed(supertype);
	}

	@SuppressWarnings("unchecked")
	@SafeVarargs
	public static <E> Argument<? extends E>[] bind(
			Parameter<? extends E>... elems) {
		return arrayMap(elems, Argument.class, e -> bind(e));
	}

	public static Argument<?>[] bind(Type<?>[] types, Parameter<?>... hints) {
		if (types.length == 0)
			return NO_PARAMS;
		Argument<?>[] args = new Argument<?>[types.length];
		for (Parameter<?> hint : hints) {
			int i = indexForType(types, hint, args);
			if (i < 0)
				throw InconsistentBinding.incomprehensiveHint(hint);
			args[i] = bind(hint);
		}
		for (int i = 0; i < args.length; i++)
			if (args[i] == null)
				args[i] = bind(types[i]);
		return args;
	}

	private static int indexForType(Type<?>[] types, Parameter<?> hint,
			Argument<?>[] args) {
		for (int i = 0; i < types.length; i++)
			if (args[i] == null && hint.type().isAssignableTo(types[i]))
				return i;
		return -1;
	}

	// ------------------------------------------------------

	public final ParameterResolution resolution;
	public final Type<T> asType;
	public final Instance<? extends T> reference;
	public final T constant;
	public final Supplier<? extends T> supplier;

	public Argument(ParameterResolution resolution, Type<T> asType,
			Instance<? extends T> reference, T constant,
			Supplier<? extends T> supplier) {
		this.resolution = asType.rawType == Injector.class
			? ParameterResolution.HIERARCHICAL
			: resolution;
		this.asType = asType;
		this.reference = reference;
		this.constant = constant;
		this.supplier = supplier;
	}

	@Override
	public Type<T> type() {
		return asType;
	}

	public Argument<T> external() {
		return new Argument<>(ParameterResolution.HIERARCHICAL, asType,
				reference, constant, supplier);
	}

	/**
	 * @param type The new type of this {@link Argument}
	 * @throws ClassCastException In case the given type is incompatible with
	 *             the previous one.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <E> Argument<E> typed(Type<E> type) {
		asType.toSupertype(type);
		return new Argument<>(resolution, type, (Instance<E>) reference,
				(E) constant, (Supplier<? extends E>) supplier);
	}

	@Override
	public String toString() {
		return Supply.describe(asType, supplier);
	}

}
