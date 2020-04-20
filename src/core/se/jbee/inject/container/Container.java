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
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Scoping.scopingOf;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.arrayFilter;
import static se.jbee.inject.Utils.arrayFindFirst;
import static se.jbee.inject.Utils.arrayFlatmap;
import static se.jbee.inject.Utils.arrayOf;
import static se.jbee.inject.container.Cast.initialiserTypeOf;
import static se.jbee.inject.container.Cast.resourcesTypeFor;

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
import se.jbee.inject.Env;
import se.jbee.inject.Generator;
import se.jbee.inject.InconsistentDeclaration;
import se.jbee.inject.Injector;
import se.jbee.inject.Locator;
import se.jbee.inject.Name;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.Scoping;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;

/**
 * The {@link Container} is not a single type or entity but the composition of
 * an {@link Injector} and it's {@link Generator}s or {@link Resource}s.
 * 
 * So while this class contains the implementation it is not a relevant type on
 * its own. Here it works more as a namespace for the implementation.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Container {

	public static Injector injector(Injectee<?>... injectees) {
		InjectorImpl impl = new InjectorImpl(injectees);
		initEager(impl);
		return impl.getDecorated();
	}

	private static void initEager(Iterable<Resource<?>[]> byType) {
		for (Resource<?>[] sameType : byType)
			for (Resource<?> eager : sameType)
				initEager(eager);
	}

	private static <T> void initEager(Resource<T> resource) {
		if (resource.scoping.isEager())
			resource.generator.generate(resource.locator.toDependency());
	}

	private Container() {
		throw new UnsupportedOperationException("util");
	}

	/**
	 * The default {@link Injector}.
	 *
	 * For each raw type ({@link Class}) all production rules ({@link Resource}
	 * s) are given ordered from most precise to least precise. The first in
	 * order that matches yields the result instance.
	 */
	@SuppressWarnings("squid:S1200")
	private static final class InjectorImpl
			implements Injector, Env, Iterable<Resource<?>[]> {

		private static final Resource<?>[] noResources = new Resource[0];

		final int generators;
		private final Map<Class<?>, Resource<?>[]> resourcesByType;
		private final Resource<?>[] genericResources;
		final Resource<? extends Initialiser<?>>[] postConstruct;
		private Initialiser<Injector>[] injectorInitialisers;
		private final Injector initialised;
		final SingletonListener singletonListener;

		InjectorImpl(Injectee<?>... injectees) {
			this.generators = injectees.length;
			this.resourcesByType = initFrom(injectees);
			this.genericResources = genericResources(resourcesByType);
			this.postConstruct = initInitialisers();
			this.singletonListener = initSingletonListeners();
			// run initialisers for the injector
			Injector decorated = this;
			if (injectorInitialisers != null) {
				for (Initialiser<Injector> init : injectorInitialisers)
					decorated = init.init(decorated, this);
				injectorInitialisers = null; // no longer needed
			}
			this.initialised = decorated;
		}

		Injector getDecorated() {
			return initialised;
		}

		private SingletonListener initSingletonListeners() {
			SingletonListener[] listeners = resolve(SingletonListener[].class);
			if (listeners.length == 0)
				return null;
			if (listeners.length == 1)
				return listeners[0];
			return new SingletonListener() {
				@Override
				public <T> void onSingletonCreated(int serialID,
						Locator<T> locator, Scoping scoping, T instance) {
					for (SingletonListener l : listeners)
						l.onSingletonCreated(serialID, locator, scoping,
								instance);
				}
			};
		}

		@Override
		public Iterator<Resource<?>[]> iterator() {
			return resourcesByType.values().iterator();
		}

		private Resource<? extends Initialiser<?>>[] initInitialisers() {
			Resource<? extends Initialiser<?>>[] inits = resolve(
					resourcesTypeFor(initialiserTypeOf(Type.WILDCARD)));
			if (inits.length == 0)
				return inits;
			Type<Initialiser<Injector>> injectorInitType = initialiserTypeOf(
					Injector.class);
			injectorInitialisers = resolve(
					injectorInitType.addArrayDimension());
			if (inits.length == injectorInitialisers.length)
				return copyOf(inits, 0); // no other dynamic initialisers
			return arrayFilter(inits, c -> !c.type().equalTo(injectorInitType));
		}

		private <T> Map<Class<?>, Resource<?>[]> initFrom(
				Injectee<?>... injectees) {
			Resource<?>[] list = new Resource<?>[injectees.length];
			for (int i = 0; i < injectees.length; i++) {
				@SuppressWarnings("unchecked")
				Injectee<T> injectee = (Injectee<T>) injectees[i];
				Name scope = injectee.scope;
				Scoping scoping = scopingOf(scope);
				Locator<T> locator = injectee.locator;
				Generator<T> gen = generator(i, injectee, scope, scoping,
						locator);
				list[i] = new Resource<>(i, injectee.source, scoping, locator,
						gen);
			}
			Arrays.sort(list);
			Map<Class<?>, Resource<?>[]> byType = new IdentityHashMap<>(
					list.length);
			if (list.length == 0)
				return byType;
			Class<?> lastRawType = list[0].type().rawType;
			int start = 0;
			for (int i = 0; i < list.length; i++) {
				Class<?> rawType = list[i].type().rawType;
				if (rawType != lastRawType) {
					byType.put(lastRawType, copyOfRange(list, start, i));
					start = i;
				}
				lastRawType = rawType;
			}
			byType.put(lastRawType, copyOfRange(list, start, list.length));
			return byType;
		}

		@SuppressWarnings("unchecked")
		private <T> Generator<T> generator(int serialID, Injectee<T> injectee,
				Name scope, Scoping scoping, Locator<T> locator) {
			Supplier<? extends T> supplier = injectee.supplier;
			if (Generator.class.isAssignableFrom(supplier.getClass()))
				return (Generator<T>) supplier;
			if (Scope.class.isAssignableFrom(locator.type().rawType)
				|| Scope.container.equalTo(scope))
				return new LazySingletonGenerator<>(this, supplier,
						injectee.locator);
			if (Scope.reference.equalTo(scope))
				return new ReferenceGenerator<>(this, supplier,
						injectee.locator);
			return new LazyScopedGenerator<>(serialID, this, injectee, scoping);
		}

		private static Resource<?>[] genericResources(
				Map<Class<?>, Resource<?>[]> byType) {
			List<Resource<?>> res = new ArrayList<>();
			for (Resource<?>[] forType : byType.values())
				for (Resource<?> resource : forType) {
					Type<?> type = resource.type();
					if (type.isUpperBound()
						|| type.isParameterizedAsUpperBound())
						res.add(resource);
				}
			Collections.sort(res);
			return res.isEmpty() ? null : res.toArray(new Resource[res.size()]);
		}

		@Override
		public <T> T property(Name name, Type<T> property, Package pkg) {
			try {
				return resolve(dependency(instance(name, property)) //
						.injectingInto(pkg));
			} catch (UnresolvableDependency e) {
				throw new InconsistentDeclaration(e);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T resolve(Dependency<T> dep) {
			final Type<T> type = dep.type();
			final Class<T> rawType = type.rawType;
			if (rawType == Injector.class
				&& (dep.instance.name.isAny() || dep.instance.name.isDefault()))
				return (T) initialised;
			if (rawType == Env.class && dep.instance.name.equalTo(Name.AS))
				return (T) this;
			// Resource based...
			boolean isResourceResolution = rawType == Resource.class
				|| rawType == Generator.class;
			if (isResourceResolution) {
				Resource<?> res = resourcesMatching(dep.onTypeParameter());
				if (res != null)
					return (T) res;
			} else {
				Resource<T> match = resourcesMatching(dep);
				if (match != null)
					return match.generate(dep);
			}
			if (type.arrayDimensions() == 1)
				return resolveArray(dep, type.baseType());
			if (isResourceResolution) {
				return (T) resolveFromUpperBound(dep.onTypeParameter());
			}
			return (T) resolveFromUpperBound(dep).generate(
					(Dependency<Object>) dep);
		}

		/**
		 * There is no direct match for the required type but there might be a
		 * wild-card binding, that is a binding capable of producing all
		 * sub-types of a certain super-type.
		 */
		private <T> Resource<?> resolveFromUpperBound(Dependency<T> dep) {
			Type<T> type = dep.type();
			Resource<?> match = arrayFindFirst(genericResources,
					c -> type.isAssignableTo(c.type()));
			if (match != null)
				return match;
			throw noResourceFor(dep);
		}

		private <T> Resource<T> resourcesMatching(Dependency<T> dep) {
			return mostQualifiedMatch(resourcesForType(dep.type()), dep);
		}

		private static <T> Resource<T> mostQualifiedMatch(Resource<T>[] rs,
				Dependency<T> dep) {
			return arrayFindFirst(rs, rx -> rx.locator.isMatching(dep));
		}

		private <T> NoResourceForDependency noResourceFor(Dependency<T> dep) {
			Type<T> type = dep.type();
			Type<?> listType = type.rawType == Resource.class
				|| type.rawType == Generator.class ? type.parameter(0) : type;
			return new NoResourceForDependency("", dep,
					resourcesForType(listType));
		}

		@SuppressWarnings("unchecked")
		private <T, E> T resolveArray(Dependency<T> dep, Type<E> elemType) {
			final Class<E> rawElemType = elemType.rawType;
			if (rawElemType == Resource.class || rawElemType == Generator.class)
				return (T) resolveResources(dep, elemType.parameter(0));
			if (dep.type().rawType.getComponentType().isPrimitive())
				throw new NoResourceForDependency(
						"Primitive arrays cannot be used to inject all instances of the wrapper type. Use the wrapper array instead.",
						dep);
			Set<Integer> identities = new HashSet<>();
			if (!elemType.isUpperBound()) {
				List<E> elements = new ArrayList<>();
				Resource<E>[] elemResources = resourcesForType(elemType);
				if (elemResources != null)
					addAllMatching(elements, identities, dep, elemType,
							elemResources);
				return toArray(elements, elemType);
			}
			List<E> elements = new ArrayList<>();
			for (Entry<Class<?>, Resource<?>[]> e : resourcesByType.entrySet())
				if (Type.raw(e.getKey()).isAssignableTo(elemType))
					addAllMatching(elements, identities, dep, elemType,
							(Resource<? extends E>[]) e.getValue());
			return toArray(elements, elemType);
		}

		@SuppressWarnings("unchecked")
		private <T, G> Resource<G>[] resolveResources(Dependency<T> dep,
				Type<G> generatedType) {
			Dependency<G> generatedTypeDep = dep.typed(generatedType);
			if (generatedType.isUpperBound())
				return resolveGenericResources(generatedType, generatedTypeDep);
			Resource<G>[] candidates = resourcesForType(generatedType);
			return candidates == null
				? (Resource<G>[]) noResources
				: arrayFilter(candidates,
						c -> c.locator.isCompatibleWith(generatedTypeDep));
		}

		@SuppressWarnings("unchecked")
		private <G> Resource<G>[] resolveGenericResources(Type<G> generatedType,
				Dependency<G> generatedTypeDep) {
			List<Resource<?>> res = new ArrayList<>();
			for (Entry<Class<?>, Resource<?>[]> e : resourcesByType.entrySet())
				if (raw(e.getKey()).isAssignableTo(generatedType))
					addCompatibleResources(res, generatedTypeDep,
							(Resource<? extends G>[]) e.getValue());
			return toArray(res, raw(Resource.class));
		}

		private static <G> void addCompatibleResources(List<Resource<?>> res,
				Dependency<G> dep, Resource<? extends G>[] candidates) {
			for (Resource<? extends G> candidate : candidates)
				if (candidate.locator.isCompatibleWith(dep))
					res.add(candidate);
		}

		private static <E, T> void addAllMatching(List<E> elements,
				Set<Integer> identities, Dependency<T> dep, Type<E> elementType,
				Resource<? extends E>[] elementResources) {
			Dependency<E> elemDep = dep.typed(elementType);
			for (int i = 0; i < elementResources.length; i++) {
				Resource<? extends E> elemResource = elementResources[i];
				if (elemResource.locator.isMatching(elemDep)) {
					E instance = elemResource.generate(elemDep);
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
		private <T> Resource<T>[] resourcesForType(Type<T> type) {
			return (Resource<T>[]) resourcesByType.get(type.rawType);
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			for (Entry<Class<?>, Resource<?>[]> e : resourcesByType.entrySet())
				toString(b, e.getKey().toString(), e.getValue());
			if (genericResources != null)
				toString(b, "? extends *", genericResources);
			return b.toString();
		}

		private static void toString(StringBuilder b, String group,
				Resource<?>[] rs) {
			b.append(group).append('\n');
			for (Resource<?> rx : rs) {
				Locator<?> r = rx.locator;
				b.append("\t#").append(rx.serialID).append(' ');
				b.append(r.type().simpleName()).append(' ');
				b.append(r.instance.name).append(' ');
				b.append(r.target).append(' ');
				b.append(rx.scoping).append(' ');
				b.append(rx.source).append('\n');
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
		private final Locator<T> locator;

		LazySingletonGenerator(Injector injector,
				Supplier<? extends T> supplier, Locator<T> locator) {
			this.injector = injector;
			this.supplier = supplier;
			this.locator = locator;
		}

		@Override
		public T generate(Dependency<? super T> dep)
				throws UnresolvableDependency {
			dep.ensureNoIllegalDirectAccessOf(locator);
			return value.get(() -> supplier.supply(
					dep.injectingInto(locator, Scoping.singleton), injector));
		}
	}

	/**
	 * Special {@link Generator} for forward referencing {@link Resource}s.
	 * These are created with {@link Scope#reference}.
	 *
	 * @param <T> Type of the generated value
	 */
	private static final class ReferenceGenerator<T> implements Generator<T> {
		private final Injector injector;
		private final Supplier<? extends T> supplier;
		private final Locator<T> locator;

		ReferenceGenerator(Injector injector, Supplier<? extends T> supplier,
				Locator<T> locator) {
			this.injector = injector;
			this.supplier = supplier;
			this.locator = locator;
		}

		@Override
		public T generate(Dependency<? super T> dep)
				throws UnresolvableDependency {
			dep.ensureNoIllegalDirectAccessOf(locator);
			return supplier.supply(dep.injectingInto(locator, Scoping.ignore),
					injector);
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
		private final Locator<T> locator;
		private final Lazy<Scope> scope = new Lazy<>();

		private Class<?> cachedForType;
		private Initialiser<? super T>[] cachedPostConstructs;

		LazyScopedGenerator(int serialID, InjectorImpl injector,
				Injectee<T> injectee, Scoping scoping) {
			this.serialID = serialID;
			this.injector = injector;
			this.scoping = scoping;
			this.supplier = injectee.supplier;
			this.locator = injectee.locator;
		}

		private Scope resolveScope() {
			return injector.resolve(scoping.scope, Scope.class);
		}

		@Override
		public T generate(Dependency<? super T> dep) {
			dep.ensureNoIllegalDirectAccessOf(locator);
			final Dependency<? super T> injected = dep.injectingInto(locator,
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
			return scope.get(this::resolveScope).provide(serialID, injected,
					() -> // 
					instanceCache.updateAndGet(instance -> instance != null
						? instance
						: createInstance(injected)),
					injector.generators);
		}

		private T createInstance(Dependency<? super T> injected) {
			T instance = supplier.supply(injected, injector());
			if (instance != null && injector.postConstruct != null
				&& injector.postConstruct.length > 0)
				instance = postConstruct(instance, injected);
			if (scoping.isStableByDesign()
				&& injector.singletonListener != null) {
				injector.singletonListener.onSingletonCreated(serialID, locator,
						scoping, instance);
			}
			return instance;
		}

		private Injector injector() {
			Injector initialised = injector.getDecorated();
			return initialised != null ? initialised : injector;
		}

		@SuppressWarnings("unchecked")
		private T postConstruct(T instance, Dependency<?> context) {
			Class<T> actualType = (Class<T>) instance.getClass();
			if (actualType == Class.class || actualType == Resource.class
				|| Initialiser.class.isAssignableFrom(actualType)) {
				return instance;
			}
			if (actualType != cachedForType) {
				cachedForType = actualType;
				cachedPostConstructs = arrayFlatmap(injector.postConstruct,
						Initialiser.class,
						rx -> matchAndGenerateInitialiser(actualType, rx, context));
			}
			if (cachedPostConstructs.length > 0)
				for (Initialiser<? super T> ix : cachedPostConstructs)
					instance = (T) ix.init(instance, injector());
			return instance;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		private <I extends Initialiser<?>> Initialiser<? super T> matchAndGenerateInitialiser(
				Class<T> actualType, Resource<I> resource, Dependency<?> context) {
			Locator<I> initialiser = resource.locator;
			if (!initialiser.target.isAvailableFor(context))
				return null;
			Type<?> required = initialiser.type().parameter(0);
			if (!raw(actualType).isAssignableTo(required))
				return null;
			Type<?> provided = Type.supertype(required.rawType,
					(Type) Type.classType(actualType));
			if (!provided.isAssignableTo(required))
				return null;
			return (Initialiser<? super T>) resource.generate(
					dependency(initialiser.instance));
		}
	}
}
