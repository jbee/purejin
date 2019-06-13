/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Type.parameterTypes;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.newArray;
import static se.jbee.inject.bootstrap.Argument.bind;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import se.jbee.inject.Dependency;
import se.jbee.inject.InjectionCase;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Parameter;
import se.jbee.inject.Provider;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.UnresolvableDependency.NoCaseForDependency;
import se.jbee.inject.UnresolvableDependency.SupplyFailed;
import se.jbee.inject.container.Supplier;

/**
 * Utility as a factory to create different kinds of {@link Supplier}s.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Supply {

	public static final Supplier<Provider<?>> PROVIDER_BRIDGE = new ProviderSupplierBridge();
	public static final Supplier<List<?>> LIST_BRIDGE = new ArrayToListBridge();
	public static final Supplier<Set<?>> SET_BRIDGE = new ArrayToSetBridge();
	public static final Supplier<Logger> LOGGER = new LoggerSupplier();

	private static final Supplier<?> REQUIRED = new RequiredSupplier<>();

	private static final String SUPPLIES = "supplies";

	/**
	 * A {@link Supplier} used as fall-back. Should a required resource not be
	 * provided it is still bound to this supplier that will throw an exception
	 * should it ever be called.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Supplier<T> required() {
		return (Supplier<T>) REQUIRED;
	}

	public static <T> Supplier<T> constant(T constant) {
		return new ConstantSupplier<>(constant);
	}

	public static <T> Supplier<T> reference(
			Class<? extends Supplier<? extends T>> type) {
		return new SupplierSupplierBridge<>(type);
	}

	public static <E> Supplier<E[]> elements(Type<E[]> arrayType,
			Parameter<? extends E>[] elements) {
		return new PredefinedArraySupplier<>(arrayType,
				Argument.bind(elements));
	}

	public static <T> Supplier<T> instance(Instance<T> instance) {
		return new LazyInstance<>(instance);
	}

	public static <T> Supplier<T> dependency(Dependency<T> dependency) {
		return new LazyDependency<>(dependency);
	}

	public static <T> Supplier<T> parametrizedInstance(Instance<T> instance) {
		return new LazyParametrizedInstance<>(instance);
	}

	public static <T> Supplier<T> method(Factory<T> method) {
		return new MethodSupplier<>(method,
				bind(parameterTypes(method.target), method.hints));
	}

	public static <T> Supplier<T> constructor(New<T> constructor) {
		return new ConstructorSupplier<>(constructor.target,
				bind(parameterTypes(constructor.target), constructor.hints));
	}

	public static <T> Provider<T> lazyProvider(Dependency<T> dependency,
			Injector injector) {
		return dependency.type().arrayDimensions() == 1
			// composed within the Injector
			? new LazyDirectProvider<>(dependency, injector)
			: new LazyPreresolvedProvider<>(dependency, injector);
	}

	private Supply() {
		throw new UnsupportedOperationException("util");
	}

	public abstract static class ArrayBridge<T> implements Supplier<T> {

		ArrayBridge() {
			// make visible
		}

		@Override
		public final T supply(Dependency<? super T> dep, Injector context) {
			Type<?> elementType = dep.type().parameter(0);
			return bridge(supplyArray(
					dep.typed(elementType.addArrayDimension()), context));
		}

		private static <E> E[] supplyArray(Dependency<E[]> elementType,
				Injector resolver) {
			return resolver.resolve(elementType);
		}

		abstract <E> T bridge(E[] elements);
	}

	/**
	 * Shows how support for {@link List}s and such works.
	 *
	 * Basically we just resolve the array of the element type (generic of the
	 * list). Arrays itself have build in support that will (if not redefined by
	 * a more precise binding) return all known
	 *
	 * @author Jan Bernitt (jan@jbee.se)
	 *
	 */
	private static final class ArrayToListBridge extends ArrayBridge<List<?>> {

		ArrayToListBridge() {
			// make visible
		}

		@Override
		<E> List<E> bridge(E[] elements) {
			return Arrays.asList(elements);
		}

	}

	private static final class ArrayToSetBridge extends ArrayBridge<Set<?>> {

		ArrayToSetBridge() {
			// make visible
		}

		@Override
		<E> Set<E> bridge(E[] elements) {
			return new HashSet<>(Arrays.asList(elements));
		}

	}

	private static final class LazyDependency<T> implements Supplier<T> {

		private final Dependency<T> dependency;

		LazyDependency(Dependency<T> dependency) {
			this.dependency = dependency;
		}

		@Override
		public T supply(Dependency<? super T> dep, Injector context) {
			return context.resolve(this.dependency);
		}

		@Override
		public String toString() {
			return describe(SUPPLIES, dependency);
		}
	}

	private static final class ConstantSupplier<T> implements Supplier<T> {

		private final T constant;

		ConstantSupplier(T constant) {
			this.constant = constant;
		}

		@Override
		public T supply(Dependency<? super T> dep, Injector context) {
			return constant;
		}

		@Override
		public String toString() {
			return describe(SUPPLIES, constant);
		}

	}

	/**
	 * A {@link Supplier} uses multiple different separate suppliers to provide
	 * the elements of a array of the supplied type.
	 *
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	private static final class PredefinedArraySupplier<E>
			extends WithParameters<E[]> {

		private final Type<E[]> arrayType;
		private final E[] res;

		@SuppressWarnings("unchecked")
		PredefinedArraySupplier(Type<E[]> arrayType,
				Argument<? extends E>[] elements) {
			super(elements);
			this.arrayType = arrayType;
			this.res = (E[]) newArray(arrayType.baseType().rawType,
					elements.length);
		}

		@Override
		protected void init(Dependency<? super E[]> dependency,
				Injector injector) {
			/* NOOP */ }

		@Override
		protected E[] invoke(Object[] args) {
			System.arraycopy(args, 0, res, 0, res.length);
			return res;
		}

		@Override
		public String toString() {
			return describe(SUPPLIES, arrayType);
		}
	}

	private static final class SupplierSupplierBridge<T>
			implements Supplier<T> {

		private final Class<? extends Supplier<? extends T>> type;

		SupplierSupplierBridge(Class<? extends Supplier<? extends T>> type) {
			this.type = type;
		}

		@Override
		public T supply(Dependency<? super T> dep, Injector context) {
			final Supplier<? extends T> supplier = context.resolve(
					dep.instanced(anyOf(type)));
			return supplier.supply(dep, context);
		}
	}

	/**
	 * E.g. used to "forward" Collection<T> to List<T>.
	 */
	private static final class LazyParametrizedInstance<T>
			implements Supplier<T> {

		private final Instance<? extends T> instance;

		LazyParametrizedInstance(Instance<? extends T> instance) {
			this.instance = instance;
		}

		@Override
		public T supply(Dependency<? super T> dep, Injector context) {
			Type<? super T> type = dep.type();
			Instance<? extends T> parametrized = instance.typed(
					instance.type().parametized(type.parameters()).upperBound(
							dep.type().isUpperBound()));
			return context.resolve(dep.instanced(parametrized));
		}

		@Override
		public String toString() {
			return describe(SUPPLIES, instance);
		}

	}

	private static final class LazyInstance<T> implements Supplier<T> {

		private final Instance<? extends T> instance;

		LazyInstance(Instance<? extends T> instance) {
			this.instance = instance;
		}

		@Override
		public T supply(Dependency<? super T> dep, Injector context) {
			// Note that this is not "buffered" using specs as it is used to
			// implement the plain resolution
			return context.resolve(dep.instanced(instance));
		}

		@Override
		public String toString() {
			return describe(SUPPLIES, instance);
		}
	}

	private static final class ProviderSupplierBridge
			implements Supplier<Provider<?>> {

		ProviderSupplierBridge() {
			// make visible
		}

		@Override
		public Provider<?> supply(Dependency<? super Provider<?>> dep,
				Injector context) {
			return lazyProvider(
					dep.onTypeParameter().uninject().ignoredScoping(), context);
		}

		@Override
		public String toString() {
			return describe(SUPPLIES, Provider.class);
		}
	}

	private static final class LazyDirectProvider<T> implements Provider<T> {

		private final Dependency<T> dependency;
		private final Injector injector;

		LazyDirectProvider(Dependency<T> dependency, Injector injector) {
			this.dependency = dependency;
			this.injector = injector;
		}

		@Override
		public T provide() throws UnresolvableDependency {
			return injector.resolve(dependency);
		}

	}

	private static final class LazyPreresolvedProvider<T>
			implements Provider<T> {

		private final Dependency<T> dependency;
		private final InjectionCase<? extends T> icase;

		@SuppressWarnings("unchecked")
		LazyPreresolvedProvider(Dependency<T> dep, Injector injector) {
			this.dependency = dep;
			this.icase = injector.resolve(dep.typed(
					raw(InjectionCase.class).parametized(dep.type())));
		}

		@Override
		public T provide() {
			return icase.yield(dependency);
		}

		@Override
		public String toString() {
			return describe("provides", dependency);
		}
	}

	private static final class ConstructorSupplier<T>
			extends WithParameters<T> {

		private final Constructor<T> target;

		ConstructorSupplier(Constructor<T> target, Argument<?>[] params) {
			super(params);
			this.target = target;
		}

		@Override
		protected void init(Dependency<? super T> dependency,
				Injector injector) {
			/* NOOP */}

		@Override
		protected T invoke(Object[] args) {
			return Supply.construct(target, args);
		}

		@Override
		public String toString() {
			return describe(target);
		}
	}

	private static final class MethodSupplier<T> extends WithParameters<T> {

		private Object owner;
		private final Factory<T> method;
		private final Class<T> returns;

		MethodSupplier(Factory<T> method, Argument<?>[] parameters) {
			super(parameters);
			this.method = method;
			this.returns = method.returns.rawType;
			this.owner = method.owner;
		}

		@Override
		protected void init(Dependency<? super T> dependency,
				Injector injector) {
			if (method.isInstanceMethod && owner == null) {
				owner = injector.resolve(method.target.getDeclaringClass());
			}
		}

		@Override
		protected T invoke(Object[] args) {
			return returns.cast(Supply.produce(method.target, owner, args));
		}

		@Override
		public String toString() {
			return describe(method.target);
		}
	}

	private static class RequiredSupplier<T> implements Supplier<T> {

		RequiredSupplier() {
			// make visible
		}

		@Override
		public T supply(Dependency<? super T> dep, Injector context) {
			throw required(dep);
		}

		@SuppressWarnings("unchecked")
		private static <T> NoCaseForDependency required(
				Dependency<T> dependency) {
			return new NoCaseForDependency(dependency, new InjectionCase[0],
					"Should never be called!");
		}

		@Override
		public String toString() {
			return Supply.describe("required");
		}
	}

	private static class LoggerSupplier implements Supplier<Logger> {

		LoggerSupplier() {
			// make visible
		}

		@Override
		public Logger supply(Dependency<? super Logger> dep, Injector context) {
			return Logger.getLogger(
					dep.target(1).type().rawType.getCanonicalName());
		}

	}

	public static String describe(Object behaviour) {
		return "<" + behaviour + ">";
	}

	public static String describe(Object behaviour, Object variant) {
		return "<" + behaviour + ":" + variant + ">";
	}

	public static String describe(Object behaviour, Object[] variants) {
		return describe(behaviour, Arrays.toString(variants));
	}

	public abstract static class WithParameters<T> implements Supplier<T> {

		private final Argument<?>[] params;

		private InjectionSite previous;

		WithParameters(Argument<?>[] params) {
			this.params = params;

		}

		protected abstract void init(Dependency<? super T> dep,
				Injector injector);

		protected abstract T invoke(Object[] args);

		@Override
		public T supply(Dependency<? super T> dep, Injector context)
				throws UnresolvableDependency {
			// this is important so previous might work as a simple cache but
			// never causes trouble for this invocation in face of multiple
			// threads calling
			InjectionSite local = previous;
			if (local == null) {
				init(dep, context);
			}
			if (local == null || !local.site.equalTo(dep)) {
				local = new InjectionSite(context, dep, params);
				previous = local;
			}
			return invoke(local.args(context));
		}

	}

	public static <T> T construct(Constructor<T> target, Object... args)
			throws SupplyFailed {
		try {
			return target.newInstance(args);
		} catch (Exception e) {
			throw SupplyFailed.valueOf(e, target);
		}
	}

	public static Object produce(Method target, Object owner, Object... args)
			throws SupplyFailed {
		try {
			return target.invoke(owner, args);
		} catch (Exception e) {
			throw SupplyFailed.valueOf(e, target);
		}
	}
}
