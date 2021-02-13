/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.binder;

import se.jbee.inject.*;
import se.jbee.inject.config.Invoke;
import se.jbee.inject.config.New;
import se.jbee.lang.Type;
import se.jbee.lang.TypeVariable;

import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.function.UnaryOperator;

import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.anyOf;
import static se.jbee.lang.Type.raw;
import static se.jbee.lang.Utils.newArray;

/**
 * Utility as a factory to create different kinds of {@link Supplier}s.
 */
public final class Supply {

	public static final Supplier<Provider<?>> PROVIDER = (dep, context) //
	-> byLazyProvider(dep.onTypeParameter().uninject().ignoredScoping(),
			context);

	private static final Supplier<?> REQUIRED = (dep, context) -> {
		throw new UnresolvableDependency.ResourceResolutionFailed("Should never be called!", dep);
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
		return (dep, context) -> context.resolve(dep.onInstance(anyOf(type))) //
				.supply(dep, context);
	}

	public static <E> Supplier<E[]> byElementReferences(Type<E[]> arrayType,
			Hint<? extends E>[] elements) {
		return new ArrayElementReferencesSupplier<>(arrayType, elements);
	}

	public static <T> Supplier<T> byInstanceReference(Instance<T> instance) {
		// Note that this is not "buffered" using Resources as it is used to
		// implement the plain resolution
		return (dep, context) -> context.resolve(dep.onInstance(instance));
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
			return context.resolve(dep.onInstance(parametrized));
		};
	}

	public static <T> Supplier<T> byProduction(Produces<T> produces) {
		return new Produce<>(produces);
	}

	public static <T> Supplier<T> byConstruction(Constructs<T> constructs) {
		return new Construct<>(constructs);
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
	private static final class ArrayElementReferencesSupplier<E>
			extends WithArgs<E[]> {

		private final Type<E[]> arrayType;
		private final Hint<? extends E>[] elements;

		ArrayElementReferencesSupplier(Type<E[]> arrayType,
				Hint<? extends E>[] elements) {
			this.arrayType = arrayType;
			this.elements = elements;
		}

		@SuppressWarnings("SuspiciousSystemArraycopy")
		@Override
		protected E[] invoke(Object[] args, Injector context) {
			@SuppressWarnings("unchecked")
			E[] res = (E[]) newArray(arrayType.rawType.getComponentType(), args.length);
			System.arraycopy(args, 0, res, 0, res.length);
			return res;
		}

		@Override
		protected Hint<?>[] actualParametersFor(Dependency<? super E[]> dep, Injector context) {
			return elements;
		}

		@Override
		public String toString() {
			return "array " + arrayType + ": " + Arrays.toString(elements);
		}
	}

	private static final class Construct<T> extends WithArgs<T>
			implements Annotated {

		private final Constructs<T> constructs;
		private New newInstance;

		Construct(Constructs<T> constructs) {
			this.constructs = constructs;
		}

		@Override
		public AnnotatedElement element() {
			return constructs.target;
		}

		@Override
		@SuppressWarnings("unchecked")
		protected T invoke(Object[] args, Injector context) {
			try {
				if (newInstance == null)
					newInstance = context.resolve(dependency(New.class)
							.injectingInto(constructs.target.getDeclaringClass()));
				return (T) newInstance.call(constructs.target, args);
			} catch (Exception e) {
				throw UnresolvableDependency.SupplyFailed.valueOf(e, constructs.target);
			}
		}

		@Override
		protected Hint<?>[] actualParametersFor(Dependency<? super T> dep, Injector context) {
			return constructs.actualParameters(dep.type(), context);
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + ": " + constructs;
		}
	}

	private static final class Produce<T> extends WithArgs<T>
			implements Annotated {

		private Object instance;
		private Invoke invoke;
		private final Produces<T> produces;
		private final Class<T> returns;
		private final Map<java.lang.reflect.TypeVariable<?>, UnaryOperator<Type<?>>> typeVariableResolvers;

		Produce(Produces<T> produces) {
			this.produces = produces;
			this.returns = produces.actualType.rawType;
			this.instance = produces.isHinted() ? null : produces.as;
			this.typeVariableResolvers = produces.isGeneric()
				? TypeVariable.typeVariables(
						produces.target.getGenericReturnType())
				: null;
		}

		@Override
		public AnnotatedElement element() {
			return produces.target;
		}

		@Override
		protected T invoke(Object[] args, Injector context) {
			if (instance == null && !produces.isStatic()) {
				instance = produces.isHinted()
					? produces.getAsHint().resolveIn(context)
					: context.resolve(produces.target.getDeclaringClass());
			}
			if (invoke == null)
				invoke = context.resolve(dependency(Invoke.class)
						.injectingInto(produces.target.getDeclaringClass()));
			try {
				return returns.cast(invoke.call(produces.target, instance, args));
			} catch (Exception e) {
				throw UnresolvableDependency.SupplyFailed.valueOf(e, produces.target);
			}
		}

		@Override
		protected Hint<?>[] actualParametersFor(Dependency<? super T> dep, Injector context) {
			//TODO it is not always sure that produces.actualDeclaringType() is the actual owner object type => look at dependency!
			Hint<?>[] actualParameters = produces.actualParameters(
					produces.actualDeclaringType(), context);
			if (!produces.isGeneric())
				return actualParameters;
			Hint<?>[] actualTypeHints = actualParameters.clone();
			Map<java.lang.reflect.TypeVariable<?>, Type<?>> actualTypes = TypeVariable.actualTypesFor(
					typeVariableResolvers, dep.type());
			java.lang.reflect.Parameter[] params = produces.target.getParameters();
			int i = 0;
			if (produces.isGenericTypeAware()) {
				actualTypeHints[0] = Hint.constant(dep.type());
				i++;
			}
			for (; i < actualTypeHints.length; i++) {
				actualTypeHints[i] = actualTypeHints[i] //
						.withActualType(params[i], actualTypes);
			}
			return actualTypeHints;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + ": " + produces;
		}
	}

	public abstract static class WithArgs<T> implements Supplier<T> {

		private InjectionSite previous;

		protected abstract T invoke(Object[] args, Injector context);

		protected abstract Hint<?>[] actualParametersFor(Dependency<? super T> dep, Injector context);

		@Override
		public T supply(Dependency<? super T> dep, Injector context)
				throws UnresolvableDependency {
			// this is important so previous might work as a simple cache but
			// never causes trouble for this invocation in face of multiple
			// threads calling
			InjectionSite local = previous;
			if (local == null || !local.site.equalTo(dep)) {
				local = new InjectionSite(context, dep,
						actualParametersFor(dep, context));
				previous = local;
			}
			Object[] args = local.args(context);
			return invoke(args, context);
		}
	}
}
