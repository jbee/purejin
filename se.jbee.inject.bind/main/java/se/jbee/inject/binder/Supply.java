/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.binder;

import se.jbee.inject.*;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.lang.Reflect;
import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.TypeVariable;
import se.jbee.inject.lang.Utils;

import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.lang.Type.parameterTypes;
import static se.jbee.inject.lang.Type.raw;
import static se.jbee.inject.lang.Utils.newArray;

/**
 * Utility as a factory to create different kinds of {@link Supplier}s.
 */
public final class Supply {

	public static final Supplier<Provider<?>> PROVIDER = (dep, context) //
	-> byLazyProvider(dep.onTypeParameter().uninject().ignoredScoping(),
			context);

	private static final Supplier<?> REQUIRED = (dep, context) -> {
		throw new NoResourceForDependency("Should never be called!", dep);
	};

	/**
	 * Shows how support for {@link List}s and such works.
	 *
	 * Basically we just resolve the array of the element type (generic of the
	 * list). Arrays itself have build in support that will (if not redefined by
	 * a more precise binding) return all known
	 */
	public static final ArrayBridge<List<?>> LIST_BRIDGE = Arrays::asList;
	public static final ArrayBridge<Set<?>> SET_BRIDGE = //
			elems -> new HashSet<>(Arrays.asList(elems));

	private static final String SUPPLIES = "supplies";

	/**
	 * A {@link Supplier} used as fall-back. Should a required {@link Locator}
	 * not be provided it is still bound to this supplier that will throw an
	 * exception should it ever be called.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Supplier<T> required() {
		return (Supplier<T>) REQUIRED;
	}

	/**
	 * A {@link Supplier} to {@link Supplier} bridge.
	 */
	public static <T> Supplier<T> bySupplierReference(
			Class<? extends Supplier<? extends T>> type) {
		return (dep, context) -> context.resolve(dep.instanced(anyOf(type))) //
				.supply(dep, context);
	}

	public static <E> Supplier<E[]> fromElements(Type<E[]> arrayType,
			Hint<? extends E>[] elements) {
		return new PredefinedArraySupplier<>(arrayType, elements);
	}

	public static <T> Supplier<T> byInstanceReference(Instance<T> instance) {
		// Note that this is not "buffered" using Resources as it is used to
		// implement the plain resolution
		return (dep, context) -> context.resolve(dep.instanced(instance));
	}

	public static <T> Supplier<T> byDependencyReference(
			Dependency<T> dependency) {
		return (dep, context) -> context.resolve(dependency);
	}

	/**
	 * E.g. used to "forward" a {@link Collection} to {@link List} of same
	 * element type.
	 */
	public static <T> Supplier<T> byParameterizedInstanceReference(
			Instance<T> instance) {
		return (dep, context) -> {
			Type<? super T> type = dep.type();
			Instance<? extends T> parametrized = instance.typed(instance.type() //
							.parameterized(type.parameters()) //
							.upperBound(dep.type().isUpperBound()));
			return context.resolve(dep.instanced(parametrized));
		};
	}

	public static <T> Supplier<T> byProduction(Produces<T> produces) {
		if (produces.declaresTypeParameters() && produces.requestsActualType()) {
			// use a constant null hint to blank first parameter as it is filled in with actual type on method invocation
			Hint<?> actualTypeHint = Hint.constantNull(
					Type.parameterType(produces.target.getParameters()[0]));
			return new Produce<>(produces,
					Hint.match(parameterTypes(produces.target),
							Utils.arrayPrepend(actualTypeHint, produces.hints)),
					Dependency::type);
		}
		return new Produce<>(produces,
				Hint.match(parameterTypes(produces.target), produces.hints), null);
	}

	public static <T> Supplier<T> byConstruction(Constructs<T> constructs) {
		return new Construct<>(constructs, Hint.match(
				parameterTypes(constructs.target), constructs.hints));
	}

	/**
	 * This effectively avoid redoing {@link Resource} resolution for each
	 * invocation. Instead the {@link Resource} is resolved once and
	 * continuously used from there on to {@link Resource#generate(Dependency)}
	 * the values.
	 */
	public static <T> Provider<T> byLazyProvider(Dependency<T> dep,
			Injector injector) {
		if (dep.type().arrayDimensions() == 1)
			return () -> injector.resolve(dep);
		@SuppressWarnings("unchecked")
		Resource<? extends T> resource = injector.resolve(
				dep.typed(raw(Resource.class).parameterized(dep.type())));
		return () -> resource.generate(dep);
	}

	private Supply() {
		throw new UnsupportedOperationException("util");
	}

	@FunctionalInterface
	public interface ArrayBridge<T> extends Supplier<T> {

		@Override
		default T supply(Dependency<? super T> dep, Injector context) {
			return bridge(context.resolve(
					dep.typed(dep.type().parameter(0).addArrayDimension())));
		}

		T bridge(Object[] elems);
	}

	/**
	 * A {@link Supplier} uses multiple different separate suppliers to provide
	 * the elements of a array of the supplied type.
	 */
	private static final class PredefinedArraySupplier<E>
			extends WithArgs<E[]> {

		private final Type<E[]> arrayType;

		PredefinedArraySupplier(Type<E[]> arrayType,
				Hint<? extends E>[] elements) {
			super(elements);
			this.arrayType = arrayType;
		}

		@SuppressWarnings("SuspiciousSystemArraycopy")
		@Override
		protected E[] invoke(Object[] args, Injector context) {
			@SuppressWarnings("unchecked")
			E[] res = (E[]) newArray(arrayType.baseType().rawType, args.length);
			System.arraycopy(args, 0, res, 0, res.length);
			return res;
		}

		@Override
		public String toString() {
			return describe(SUPPLIES, arrayType);
		}
	}

	private static final class Construct<T> extends WithArgs<T>
			implements Annotated {

		private final Constructs<T> constructs;

		Construct(Constructs<T> constructs, Hint<?>[] args) {
			super(args);
			this.constructs = constructs;
		}

		@Override
		public AnnotatedElement element() {
			return constructs.target;
		}

		@Override
		@SuppressWarnings("unchecked")
		protected T invoke(Object[] args, Injector context) {
			return (T) Reflect.construct(constructs.target, args,
					e -> UnresolvableDependency.SupplyFailed.valueOf(e, constructs.target));
		}

		@Override
		public String toString() {
			return describe(constructs.target);
		}
	}

	private static final class Produce<T> extends WithArgs<T>
			implements Annotated {

		private Object instance;
		private final Produces<T> producer;
		private final Class<T> returns;
		private final Map<java.lang.reflect.TypeVariable<?>, UnaryOperator<Type<?>>> typeVariableResolvers;

		Produce(Produces<T> producer, Hint<?>[] args,
				Function<Dependency<?>, Object> supplyActual) {
			super(args, supplyActual);
			this.producer = producer;
			this.returns = producer.actualType.rawType;
			this.instance = producer.isHinted() ? null : producer.as;
			this.typeVariableResolvers = producer.declaresTypeParameters()
				? TypeVariable.typeVariables(
						producer.target.getGenericReturnType())
				: null;
		}

		@Override
		public AnnotatedElement element() {
			return producer.target;
		}

		@Override
		protected T invoke(Object[] args, Injector context) {
			if (instance == null && !producer.isStatic()) {
				instance = producer.isHinted()
					? producer.getAsHint().resolveIn(context)
					: context.resolve(producer.target.getDeclaringClass());
			}
			return returns.cast(Reflect.produce(producer.target, instance, args,
					e -> UnresolvableDependency.SupplyFailed.valueOf(e, producer.target)));
		}

		@Override
		protected Hint<?>[] hintsFor(Dependency<? super T> dep) {
			if (!producer.declaresTypeParameters())
				return hints;
			Hint<?>[] actualTypeHints = hints.clone();
			Map<java.lang.reflect.TypeVariable<?>, Type<?>> actualTypes = TypeVariable.actualTypesFor(
					typeVariableResolvers, dep.type());
			java.lang.reflect.Parameter[] params = producer.target.getParameters();
			for (int i = 0; i < actualTypeHints.length; i++) {
				actualTypeHints[i] = actualTypeHints[i] //
						.withActualType(params[i], actualTypes);
			}
			return actualTypeHints;
		}

		@Override
		public String toString() {
			return describe(producer.target);
		}
	}

	public static String describe(Object behaviour) {
		return "<" + behaviour + ">";
	}

	public static String describe(Object behaviour, Object variant) {
		return "<" + behaviour + ":" + variant + ">";
	}

	public abstract static class WithArgs<T> implements Supplier<T> {

		protected final Hint<?>[] hints;
		private final Function<Dependency<?>, Object> supplyActual;
		private InjectionSite previous;

		WithArgs(Hint<?>[] params) {
			this(params, null);
		}

		WithArgs(Hint<?>[] hints,
				Function<Dependency<?>, Object> supplyActual) {
			this.hints = hints;
			this.supplyActual = supplyActual;
		}

		protected abstract T invoke(Object[] args, Injector context);

		@Override
		public T supply(Dependency<? super T> dep, Injector context)
				throws UnresolvableDependency {
			// this is important so previous might work as a simple cache but
			// never causes trouble for this invocation in face of multiple
			// threads calling
			InjectionSite local = previous;
			if (local == null || !local.site.equalTo(dep)) {
				local = new InjectionSite(context, dep, hintsFor(dep));
				previous = local;
			}
			Object[] args = local.args(context);
			if (supplyActual != null)
				args[0] = supplyActual.apply(dep);
			return invoke(args, context);
		}

		protected Hint<?>[] hintsFor(
				@SuppressWarnings("unused") Dependency<? super T> dep) {
			return hints;
		}
	}
}
