/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.binder;

import static java.util.stream.Collectors.toMap;
import static se.jbee.inject.Hint.match;
import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Type.parameterTypes;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.newArray;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;

import se.jbee.inject.Annotated;
import se.jbee.inject.Dependency;
import se.jbee.inject.Hint;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Locator;
import se.jbee.inject.Parameter;
import se.jbee.inject.Provider;
import se.jbee.inject.Resource;
import se.jbee.inject.Supplier;
import se.jbee.inject.Type;
import se.jbee.inject.TypeVariable;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.Utils;
import se.jbee.inject.container.InjectionSite;

/**
 * Utility as a factory to create different kinds of {@link Supplier}s.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Supply {

	public static final Supplier<Provider<?>> PROVIDER = (dep, context) //
	-> byLazyProvider(dep.onTypeParameter().uninject().ignoredScoping(),
			context);

	public static final Supplier<Logger> LOGGER = (dep, context) //
	-> Logger.getLogger(dep.target(1).type().rawType.getCanonicalName());

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
			Parameter<? extends E>[] elements) {
		return new PredefinedArraySupplier<>(arrayType, Hint.match(elements));
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
	public static <T> Supplier<T> byParametrizedInstanceReference(
			Instance<T> instance) {
		return (dep, context) -> {
			Type<? super T> type = dep.type();
			Instance<? extends T> parametrized = instance.typed(
					instance.type().parametized(type.parameters()).upperBound(
							dep.type().isUpperBound()));
			return context.resolve(dep.instanced(parametrized));
		};
	}

	public static <T> Supplier<T> byProducer(Produces<T> producer) {
		if (producer.hasTypeVariables && producer.requestsActualType()) {
			// use a constant null hint to blank first parameter as it is filled in with actual type on method invocation
			Hint<?> actualTypeHint = Hint.constantNull(
					Type.parameterType(producer.target.getParameters()[0]));
			return new Call<>(producer,
					match(parameterTypes(producer.target),
							Utils.arrayPrepand(actualTypeHint, producer.hints)),
					Dependency::type);
		}
		return new Call<>(producer,
				match(parameterTypes(producer.target), producer.hints), null);
	}

	public static <T> Supplier<T> byNew(New<T> instantiation) {
		return new Instantiation<>(instantiation.target, match(
				parameterTypes(instantiation.target), instantiation.hints));
	}

	public static <T> Supplier<T> byAccess(Shares<T> constant) {
		return new Access<>(constant);
	}

	public static <T> Provider<T> byLazyProvider(Dependency<T> dep,
			Injector injector) {
		if (dep.type().arrayDimensions() == 1)
			return () -> injector.resolve(dep);
		@SuppressWarnings("unchecked")
		Resource<? extends T> resource = injector.resolve(
				dep.typed(raw(Resource.class).parametized(dep.type())));
		return () -> resource.generate(dep);
	}

	private Supply() {
		throw new UnsupportedOperationException("util");
	}

	@FunctionalInterface
	public interface ArrayBridge<T> extends Supplier<T> {

		@Override
		public default T supply(Dependency<? super T> dep, Injector context) {
			return bridge(context.resolve(
					dep.typed(dep.type().parameter(0).addArrayDimension())));
		}

		T bridge(Object[] elems);
	}

	/**
	 * A {@link Supplier} uses multiple different separate suppliers to provide
	 * the elements of a array of the supplied type.
	 *
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	private static final class PredefinedArraySupplier<E>
			extends WithArgs<E[]> {

		private final Type<E[]> arrayType;

		PredefinedArraySupplier(Type<E[]> arrayType,
				Hint<? extends E>[] elements) {
			super(elements);
			this.arrayType = arrayType;
		}

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

	private static final class Access<T> implements Annotated, Supplier<T> {

		private final Shares<T> field;

		Access(Shares<T> field) {
			this.field = field;
		}

		@SuppressWarnings("unchecked")
		@Override
		public T supply(Dependency<? super T> dep, Injector context)
				throws UnresolvableDependency {
			return (T) Utils.share(field.target, field.owner);
		}

		@Override
		public AnnotatedElement element() {
			return field.target;
		}

	}

	private static final class Instantiation<T> extends WithArgs<T>
			implements Annotated {

		private final Constructor<T> target;

		Instantiation(Constructor<T> target, Hint<?>[] args) {
			super(args);
			this.target = target;
		}

		@Override
		public AnnotatedElement element() {
			return target;
		}

		@Override
		protected T invoke(Object[] args, Injector context) {
			return Utils.construct(target, args);
		}

		@Override
		public String toString() {
			return describe(target);
		}
	}

	private static final class Call<T> extends WithArgs<T>
			implements Annotated {

		private Object owner;
		private final Produces<T> producer;
		private final Class<T> returns;
		private final Map<String, UnaryOperator<Type<?>>> typeVariableResolvers;

		Call(Produces<T> producer, Hint<?>[] args,
				Function<Dependency<?>, Object> supplyActual) {
			super(args, supplyActual);
			this.producer = producer;
			this.returns = producer.returns.rawType;
			this.owner = producer.owner;
			this.typeVariableResolvers = producer.hasTypeVariables
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
			if (producer.isInstanceMethod && owner == null)
				owner = context.resolve(producer.target.getDeclaringClass());
			return returns.cast(Utils.produce(producer.target, owner, args));
		}

		@Override
		protected Hint<?>[] hintsFor(Dependency<? super T> dep) {
			if (!producer.hasTypeVariables)
				return hints;
			Hint<?>[] actualTypeHints = hints.clone();
			Map<String, Type<?>> actualTypes = typeVariableResolvers.entrySet().stream() //
					.collect(toMap(Entry::getKey,
							e -> e.getValue().apply(dep.type())));
			java.lang.reflect.Parameter[] params = producer.target.getParameters();
			for (int i = 0; i < actualTypeHints.length; i++) {
				actualTypeHints[i] = actualTypeHints[i].withActualType(
						params[i], actualTypes);
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
