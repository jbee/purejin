/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Hint.bind;
import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Type.parameterTypes;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.newArray;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import se.jbee.inject.Dependency;
import se.jbee.inject.Generator;
import se.jbee.inject.Hint;
import se.jbee.inject.Resource;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Locator;
import se.jbee.inject.Parameter;
import se.jbee.inject.Provider;
import se.jbee.inject.Scope;
import se.jbee.inject.Scoping;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.Utils;
import se.jbee.inject.container.Supplier;

/**
 * Utility as a factory to create different kinds of {@link Supplier}s.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Supply {

	public static final Supplier<Provider<?>> PROVIDER = (dep, context) //
	-> lazyProvider(dep.onTypeParameter().uninject().ignoredScoping(), context);

	public static final Supplier<Logger> LOGGER = (dep, context) //
	-> Logger.getLogger(dep.target(1).type().rawType.getCanonicalName());

	private static final Supplier<?> REQUIRED = (dep, context) -> {
		throw new NoResourceForDependency(dep, new Resource[0],
				"Should never be called!");
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

	public static <T> Supplier<T> constant(T constant) {
		return new ConstantSupplier<>(constant);
	}

	/**
	 * A {@link Supplier} => {@link Supplier} bridge.
	 */
	public static <T> Supplier<T> reference(
			Class<? extends Supplier<? extends T>> type) {
		return (dep, context) -> context.resolve(dep.instanced(anyOf(type))) //
				.supply(dep, context);
	}

	public static <E> Supplier<E[]> elements(Type<E[]> arrayType,
			Parameter<? extends E>[] elements) {
		return new PredefinedArraySupplier<>(arrayType, Hint.bind(elements));
	}

	public static <T> Supplier<T> instance(Instance<T> instance) {
		// Note that this is not "buffered" using specs as it is used to
		// implement the plain resolution
		return (dep, context) -> context.resolve(dep.instanced(instance));
	}

	public static <T> Supplier<T> dependency(Dependency<T> dependency) {
		return (dep, context) -> context.resolve(dependency);
	}

	/**
	 * E.g. used to "forward" Collection<T> to List<T>.
	 */
	public static <T> Supplier<T> parametrizedInstance(Instance<T> instance) {
		return (dep, context) -> {
			Type<? super T> type = dep.type();
			Instance<? extends T> parametrized = instance.typed(
					instance.type().parametized(type.parameters()).upperBound(
							dep.type().isUpperBound()));
			return context.resolve(dep.instanced(parametrized));
		};
	}

	public static <T> Supplier<T> method(Produces<T> method) {
		return new MethodSupplier<>(method,
				bind(parameterTypes(method.target), method.hints));
	}

	public static <T> Supplier<T> constructor(New<T> constructor) {
		return new ConstructorSupplier<>(constructor.target,
				bind(parameterTypes(constructor.target), constructor.hints));
	}

	public static <T> Provider<T> lazyProvider(Dependency<T> dep,
			Injector injector) {
		if (dep.type().arrayDimensions() == 1)
			return () -> injector.resolve(dep);
		@SuppressWarnings("unchecked")
		Resource<? extends T> resource = injector.resolve(
				dep.typed(raw(Resource.class).parametized(dep.type())));
		return () -> resource.yield(dep);
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

	/**
	 * Since the {@link Supplier} also implements {@link Generator} it is used
	 * directly without any {@link Scoping} effects. Effectively a constant
	 * always has the {@link Scope#container}.
	 * 
	 * The implementation also implements {@link #equals(Object)} and
	 * {@link #hashCode()} to allow elimination of duplicate constant bindings.
	 */
	private static final class ConstantSupplier<T>
			implements Supplier<T>, Generator<T> {

		private final T constant;

		ConstantSupplier(T constant) {
			this.constant = constant;
		}

		@Override
		public T yield(Dependency<? super T> dep)
				throws UnresolvableDependency {
			return constant;
		}

		@Override
		public T supply(Dependency<? super T> dep, Injector context)
				throws UnresolvableDependency {
			return yield(dep);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof ConstantSupplier
				&& constant == ((ConstantSupplier<?>) obj).constant;
		}

		@Override
		public int hashCode() {
			return constant == null ? super.hashCode() : constant.hashCode();
		}
	}

	private static final class ConstructorSupplier<T> extends WithArgs<T> {

		private final Constructor<T> target;

		ConstructorSupplier(Constructor<T> target, Hint<?>[] args) {
			super(args);
			this.target = target;
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

	private static final class MethodSupplier<T> extends WithArgs<T> {

		private Object owner;
		private final Produces<T> method;
		private final Class<T> returns;

		MethodSupplier(Produces<T> method, Hint<?>[] args) {
			super(args);
			this.method = method;
			this.returns = method.returns.rawType;
			this.owner = method.owner;
		}

		@Override
		protected T invoke(Object[] args, Injector context) {
			if (method.isInstanceMethod && owner == null)
				owner = context.resolve(method.target.getDeclaringClass());
			return returns.cast(Utils.produce(method.target, owner, args));
		}

		@Override
		public String toString() {
			return describe(method.target);
		}
	}

	public static String describe(Object behaviour) {
		return "<" + behaviour + ">";
	}

	public static String describe(Object behaviour, Object variant) {
		return "<" + behaviour + ":" + variant + ">";
	}

	public abstract static class WithArgs<T> implements Supplier<T> {

		private final Hint<?>[] args;
		private InjectionSite previous;

		WithArgs(Hint<?>[] args) {
			this.args = args;
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
				local = new InjectionSite(context, dep, args);
				previous = local;
			}
			return invoke(local.args(context), context);
		}

	}

}
