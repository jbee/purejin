/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.container;

import static java.lang.System.identityHashCode;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Scoping.scopingOf;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.arrayFilter;
import static se.jbee.inject.Utils.arrayFindFirst;
import static se.jbee.inject.Utils.arrayFlatmap;
import static se.jbee.inject.Utils.arrayOf;
import static se.jbee.inject.container.Cast.initialiserTypeOf;
import static se.jbee.inject.container.Cast.injectionCasesTypeFor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import se.jbee.inject.Dependency;
import se.jbee.inject.Generator;
import se.jbee.inject.InjectionCase;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.Scoping;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.UnresolvableDependency.NoCaseForDependency;

/**
 * The {@link Container} is not a single type or entity but the composition of
 * an {@link Injector} and it's {@link Generator}s or {@link InjectionCase}s.
 * 
 * So while this class contains the implementation it is not a relevant type on
 * its own. Here it works more as a namespace for the implementation.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Container {

	static final InjectionCase<?>[] noCases = new InjectionCase[0];

	public static Injector injector(Injectee<?>... injectees) {
		InjectorImpl impl = new InjectorImpl(injectees);
		initEager(impl);
		// run initialisers for the injector
		Injector res = impl;
		if (impl.injectorInitialisers != null)
			for (Initialiser<Injector> init : impl.injectorInitialisers)
				res = init.init(res, impl);
		impl.injectorInitialisers = null; // no longer needed
		impl.initialised = res;
		return res;
	}

	private static void initEager(Iterable<InjectionCase<?>[]> casesByType) {
		for (InjectionCase<?>[] cases : casesByType)
			for (InjectionCase<?> c : cases)
				initEager(c);
	}

	private static <T> void initEager(InjectionCase<T> c) {
		if (c.scoping.isEager())
			c.generator.yield(c.resource.toDependency());
	}

	private Container() {
		throw new UnsupportedOperationException("util");
	}

	/**
	 * The default {@link Injector}.
	 *
	 * For each raw type ({@link Class}) all production rules
	 * ({@link InjectionCase} s) are given ordered from most precise to least
	 * precise. The first in order that matches yields the result instance.
	 */
	@SuppressWarnings("squid:S1200")
	private static final class InjectorImpl
			implements Injector, Iterable<InjectionCase<?>[]> {

		final int generators;
		private final Map<Class<?>, InjectionCase<?>[]> casesByType;
		private final InjectionCase<?>[] wildcardCases;
		final InjectionCase<? extends Initialiser<?>>[] postConstruct;
		Initialiser<Injector>[] injectorInitialisers;
		Injector initialised;
		final YieldListener listener;

		InjectorImpl(Injectee<?>... injectees) {
			this.generators = injectees.length;
			this.casesByType = initFrom(injectees);
			this.wildcardCases = wildcardCases(casesByType);
			this.postConstruct = initInitialisers();
			this.listener = initListeners();
		}

		private YieldListener initListeners() {
			YieldListener[] listeners = resolve(YieldListener[].class);
			if (listeners.length == 0)
				return null;
			if (listeners.length == 1)
				return listeners[0];
			return new YieldListener() {
				@Override
				public <T> void onStableInstanceGeneration(int serialID,
						Resource<T> resource, Scoping scoping, T instance) {
					for (YieldListener l : listeners)
						l.onStableInstanceGeneration(serialID, resource,
								scoping, instance);
				}
			};
		}

		@Override
		public Iterator<InjectionCase<?>[]> iterator() {
			return casesByType.values().iterator();
		}

		private InjectionCase<? extends Initialiser<?>>[] initInitialisers() {
			InjectionCase<? extends Initialiser<?>>[] initCases = resolve(
					injectionCasesTypeFor(initialiserTypeOf(Type.WILDCARD)));
			if (initCases.length == 0)
				return initCases;
			Type<Initialiser<Injector>> injectorInitType = initialiserTypeOf(
					Injector.class);
			injectorInitialisers = resolve(
					injectorInitType.addArrayDimension());
			if (initCases.length == injectorInitialisers.length)
				return copyOf(initCases, 0); // no other dynamic initialisers
			return arrayFilter(initCases,
					c -> !c.type().equalTo(injectorInitType));
		}

		private <T> Map<Class<?>, InjectionCase<?>[]> initFrom(
				Injectee<?>... injectees) {
			InjectionCase<?>[] cases = new InjectionCase<?>[injectees.length];
			for (int i = 0; i < injectees.length; i++) {
				@SuppressWarnings("unchecked")
				Injectee<T> injectee = (Injectee<T>) injectees[i];
				Name scope = injectee.scope;
				Scoping scoping = scopingOf(scope);
				Resource<T> resource = injectee.resource;
				Generator<T> gen = generator(i, injectee, scope, scoping,
						resource);
				cases[i] = new InjectionCase<>(i, injectee.source, scoping,
						resource, gen);
			}
			Arrays.sort(cases);
			Map<Class<?>, InjectionCase<?>[]> casesByType = new IdentityHashMap<>(
					cases.length);
			if (cases.length == 0)
				return casesByType;
			Class<?> lastRawType = cases[0].type().rawType;
			int start = 0;
			for (int i = 0; i < cases.length; i++) {
				Class<?> rawType = cases[i].type().rawType;
				if (rawType != lastRawType) {
					casesByType.put(lastRawType, copyOfRange(cases, start, i));
					start = i;
				}
				lastRawType = rawType;
			}
			casesByType.put(lastRawType,
					copyOfRange(cases, start, cases.length));
			return casesByType;
		}

		@SuppressWarnings("unchecked")
		private <T> Generator<T> generator(int serialID, Injectee<T> injectee,
				Name scope, Scoping scoping, Resource<T> resource) {
			Supplier<? extends T> supplier = injectee.supplier;
			if (Generator.class.isAssignableFrom(supplier.getClass()))
				return (Generator<T>) supplier;
			if (Scope.class.isAssignableFrom(resource.type().rawType)
				|| Scope.container.equalTo(scope))
				return new LazySingletonGenerator<>(this, supplier);
			return new LazyScopedGenerator<>(serialID, this, injectee, scoping);
		}

		private static InjectionCase<?>[] wildcardCases(
				Map<Class<?>, InjectionCase<?>[]> cases) {
			List<InjectionCase<?>> res = new ArrayList<>();
			for (InjectionCase<?>[] casesForType : cases.values())
				for (InjectionCase<?> icase : casesForType)
					if (icase.type().isUpperBound())
						res.add(icase);
			Collections.sort(res);
			return res.isEmpty()
				? null
				: res.toArray(new InjectionCase[res.size()]);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T resolve(Dependency<T> dep) {
			final Type<T> type = dep.type();
			final Class<T> rawType = type.rawType;
			if (rawType == InjectionCase.class || rawType == Generator.class) {
				InjectionCase<?> res = injectionCaseMatching(
						dep.onTypeParameter());
				if (res != null)
					return (T) res;
			}
			if (rawType == Injector.class
				&& (dep.instance.name.isAny() || dep.instance.name.isDefault()))
				return (T) initialised;
			InjectionCase<T> match = injectionCaseMatching(dep);
			if (match != null)
				return match.yield(dep);
			if (type.arrayDimensions() == 1)
				return resolveArray(dep, type.baseType());
			return resolveFromUpperBound(dep);
		}

		/**
		 * There is no direct match for the required type but there might be a
		 * wild-card binding, that is a binding capable of producing all
		 * sub-types of a certain super-type.
		 */
		@SuppressWarnings("unchecked")
		private <T> T resolveFromUpperBound(Dependency<T> dep) {
			InjectionCase<?> match = arrayFindFirst(wildcardCases,
					c -> dep.type().isAssignableTo(c.type()));
			if (match != null)
				return (T) match.yield((Dependency<Object>) dep);
			throw noCaseFor(dep);
		}

		private <T> InjectionCase<T> injectionCaseMatching(Dependency<T> dep) {
			return mostQualifiedMatch(injectionCasesForType(dep.type()), dep);
		}

		private static <T> InjectionCase<T> mostQualifiedMatch(
				InjectionCase<T>[] cases, Dependency<T> dep) {
			return arrayFindFirst(cases,
					icase -> icase.resource.isMatching(dep));
		}

		private <T> NoCaseForDependency noCaseFor(Dependency<T> dep) {
			Type<T> type = dep.type();
			Type<?> listType = type.rawType == InjectionCase.class
				|| type.rawType == Generator.class ? type.parameter(0) : type;
			return new NoCaseForDependency(dep, injectionCasesForType(listType),
					"");
		}

		@SuppressWarnings("unchecked")
		private <T, E> T resolveArray(Dependency<T> dep, Type<E> elemType) {
			final Class<E> rawElemType = elemType.rawType;
			if (rawElemType == InjectionCase.class
				|| rawElemType == Generator.class)
				return (T) resolveCases(dep, elemType.parameter(0));
			if (dep.type().rawType.getComponentType().isPrimitive())
				throw new NoCaseForDependency(dep, null,
						"Primitive arrays cannot be used to inject all instances of the wrapper type. Use the wrapper array instead.");
			Set<Integer> identities = new HashSet<>();
			if (!elemType.isUpperBound()) {
				List<E> elements = new ArrayList<>();
				InjectionCase<E>[] elemCases = injectionCasesForType(elemType);
				if (elemCases != null)
					addAllMatching(elements, identities, dep, elemType,
							elemCases);
				return toArray(elements, elemType);
			}
			List<E> elements = new ArrayList<>();
			for (Entry<Class<?>, InjectionCase<?>[]> e : casesByType.entrySet())
				if (Type.raw(e.getKey()).isAssignableTo(elemType))
					addAllMatching(elements, identities, dep, elemType,
							(InjectionCase<? extends E>[]) e.getValue());
			return toArray(elements, elemType);
		}

		@SuppressWarnings("unchecked")
		private <T, G> InjectionCase<G>[] resolveCases(Dependency<T> dep,
				Type<G> generatedType) {
			Dependency<G> generatedTypeDep = dep.typed(generatedType);
			if (generatedType.isUpperBound())
				return resolveWildcardCases(generatedType, generatedTypeDep);
			InjectionCase<G>[] cases = injectionCasesForType(generatedType);
			return cases == null
				? (InjectionCase<G>[]) noCases
				: arrayFilter(cases,
						c -> c.resource.isCompatibleWith(generatedTypeDep));
		}

		@SuppressWarnings("unchecked")
		private <G> InjectionCase<G>[] resolveWildcardCases(
				Type<G> generatedType, Dependency<G> generatedTypeDep) {
			List<InjectionCase<?>> res = new ArrayList<>();
			for (Entry<Class<?>, InjectionCase<?>[]> e : casesByType.entrySet())
				if (raw(e.getKey()).isAssignableTo(generatedType))
					addCompatibleCases(res, generatedTypeDep,
							(InjectionCase<? extends G>[]) e.getValue());
			return toArray(res, raw(InjectionCase.class));
		}

		private static <G> void addCompatibleCases(List<InjectionCase<?>> res,
				Dependency<G> dep, InjectionCase<? extends G>[] cases) {
			for (InjectionCase<? extends G> icase : cases)
				if (icase.resource.isCompatibleWith(dep))
					res.add(icase);
		}

		private static <E, T> void addAllMatching(List<E> elements,
				Set<Integer> identities, Dependency<T> dep, Type<E> elementType,
				InjectionCase<? extends E>[] elemCases) {
			Dependency<E> elemDep = dep.typed(elementType);
			for (int i = 0; i < elemCases.length; i++) {
				InjectionCase<? extends E> elemCase = elemCases[i];
				if (elemCase.resource.isMatching(elemDep)) {
					E instance = elemCase.yield(elemDep);
					if (identities.add(identityHashCode(instance)))
						elements.add(instance);
				}
			}
		}

		@SuppressWarnings("unchecked")
		private static <T, E> T toArray(List<? extends E> elements,
				Type<E> elementType) {
			return (T) arrayOf(elements, elementType.rawType);
		}

		@SuppressWarnings("unchecked")
		private <T> InjectionCase<T>[] injectionCasesForType(Type<T> type) {
			return (InjectionCase<T>[]) casesByType.get(type.rawType);
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			for (Entry<Class<?>, InjectionCase<?>[]> e : casesByType.entrySet())
				toString(b, e.getKey().toString(), e.getValue());
			if (wildcardCases != null)
				toString(b, "? extends *", wildcardCases);
			return b.toString();
		}

		private static void toString(StringBuilder b, String group,
				InjectionCase<?>[] cases) {
			b.append(group).append('\n');
			for (InjectionCase<?> icase : cases) {
				Resource<?> r = icase.resource;
				b.append("\t#").append(icase.serialID).append(' ');
				b.append(r.type().simpleName()).append(' ');
				b.append(r.instance.name).append(' ');
				b.append(r.target).append(' ');
				b.append(icase.scoping).append(' ');
				b.append(icase.source).append('\n');
			}
		}
	}

	/**
	 * The {@link Lazy} value makes sure the {@link Supplier} is only ever
	 * called once.
	 * 
	 * This {@link Generator} represents the {@link Scope#container} where the
	 * {@link LazySingletonGenerator#value} field holds the singleton value.
	 * 
	 * So in contrast to other scopes that are implemented as instances of
	 * {@link Scope} the {@link Scope#container} is "virtual". Its instances
	 * exist in the different instances of the {@link LazyScopedGenerator}.
	 * 
	 * @param <T> Type of the lazy value
	 */
	private static final class LazySingletonGenerator<T>
			implements Generator<T> {

		private final Injector injector;
		private final Supplier<? extends T> supplier;
		private final Lazy<T> value = new Lazy<>();

		LazySingletonGenerator(Injector injector,
				Supplier<? extends T> supplier) {
			this.injector = injector;
			this.supplier = supplier;
		}

		@Override
		public T yield(Dependency<? super T> dep)
				throws UnresolvableDependency {
			return value.get(() -> supplier.supply(dep, injector));
		}
	}

	/**
	 * Default {@link Generator} that uses a {@link Scope} implementation to
	 * manage the value.
	 * 
	 * @param <T> Type of the generated value
	 */
	private static final class LazyScopedGenerator<T> implements Generator<T> {

		private final InjectorImpl injector;
		private final int serialID;
		private final Supplier<? extends T> supplier;
		private final Scoping scoping;
		private final Resource<T> resource;
		private final Lazy<Scope> scope = new Lazy<>();

		private Class<?> cachedForType;
		private Initialiser<? super T>[] cachedPostConstructs;

		LazyScopedGenerator(int serialID, InjectorImpl injector,
				Injectee<T> injectee, Scoping scoping) {
			this.serialID = serialID;
			this.injector = injector;
			this.scoping = scoping;
			this.supplier = injectee.supplier;
			this.resource = injectee.resource;
		}

		private Scope resolveScope() {
			return injector.resolve(scoping.scope, Scope.class);
		}

		@Override
		public T yield(Dependency<? super T> dep) {
			dep.ensureNoIllegalDirectAccessOf(resource);
			final Dependency<? super T> injected = dep.injectingInto(resource,
					scoping);
			/**
			 * This cache makes sure that within one thread even if the provider
			 * (lambda below) is called multiple times (which can occur because
			 * methods like {@code updateAndGet} on atomics have a loop) will
			 * always yield the same instance. Different invocation of this
			 * method (yield) however can lead to multiple calls to the
			 * supplier.
			 */
			AtomicReference<T> instanceCache = new AtomicReference<>();
			return scope.get(this::resolveScope).yield(serialID, injected, () -> // 
			instanceCache.updateAndGet(instance -> instance != null
				? instance
				: createInstance(injected)), injector.generators);
		}

		private T createInstance(Dependency<? super T> injected) {
			T instance = supplier.supply(injected, injector());
			if (instance != null && injector.postConstruct != null
				&& injector.postConstruct.length > 0)
				instance = postConstruct(instance, injected);
			if (scoping.isStableByDesign() && injector.listener != null) {
				injector.listener.onStableInstanceGeneration(serialID, resource,
						scoping, instance);
			}
			return instance;
		}

		private Injector injector() {
			Injector initialised = injector.initialised;
			return initialised != null ? initialised : injector;
		}

		@SuppressWarnings("unchecked")
		private T postConstruct(T instance, Dependency<?> context) {
			Class<?> type = instance.getClass();
			if (type == Class.class || type == InjectionCase.class
				|| Initialiser.class.isAssignableFrom(type)) {
				return instance;
			}
			if (type != cachedForType) {
				cachedForType = type;
				cachedPostConstructs = arrayFlatmap(injector.postConstruct,
						Initialiser.class,
						icase -> yieldInitialiser(type, icase, context));
			}
			if (cachedPostConstructs.length > 0)
				for (Initialiser<? super T> i : cachedPostConstructs)
					instance = (T) i.init(instance, injector());
			return instance;
		}

		@SuppressWarnings("unchecked")
		private <I extends Initialiser<?>> Initialiser<? super T> yieldInitialiser(
				Class<?> type, InjectionCase<I> icase, Dependency<?> context) {
			Resource<I> initialiser = icase.resource;
			if (!initialiser.target.isAvailableFor(context))
				return null;
			// this is not 100% generic as instance the type is derived from itself could be generic in a relevant way
			// e.g. if List<String> should be initialised but not List<Integer> we just check for List here and fail later on
			if (raw(type).isAssignableTo(initialiser.type().parameter(0)))
				return (Initialiser<? super T>) icase.yield(
						dependency(initialiser.instance));
			return null;
		}
	}
}
