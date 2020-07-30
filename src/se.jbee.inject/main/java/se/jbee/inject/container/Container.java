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
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

import se.jbee.inject.Annotated;
import se.jbee.inject.Dependency;
import se.jbee.inject.Env;
import se.jbee.inject.Generator;
import se.jbee.inject.InconsistentDeclaration;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Locator;
import se.jbee.inject.Name;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.ScopePermanence;
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
		if (resource.permanence.isEager())
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
		private final Resource<? extends Initialiser<?>>[] postConstruct;
		private final SingletonListener singletonListener;
		private final Map<Class<?>, Initialiser<?>[]> postConstructByActualType = new ConcurrentHashMap<>();
		private final Injector decorated;

		InjectorImpl(Injectee<?>... injectees) {
			this.generators = injectees.length;
			this.resourcesByType = createResources(injectees);
			this.genericResources = selectGenericResources(resourcesByType);
			this.postConstruct = initInitialisers();
			this.singletonListener = initSingletonListeners();
			this.decorated = decoratedInjectorContext();
		}

		private Injector decoratedInjectorContext() {
			Injector res = this;
			@SuppressWarnings("unchecked")
			Initialiser<Injector>[] injectorPostConstruct = (Initialiser<Injector>[]) postConstructByActualType.get(
					Injector.class);
			if (injectorPostConstruct != null) {
				for (Initialiser<Injector> init : injectorPostConstruct)
					res = init.init(res, this);
				postConstructByActualType.remove(Injector.class); // no longer needed
			}
			return res;
		}

		Injector getDecorated() {
			return decorated == null ? this : decorated;
		}

		private SingletonListener initSingletonListeners() {
			SingletonListener[] listeners = resolve(SingletonListener[].class);
			if (listeners.length == 0)
				return null;
			if (listeners.length == 1)
				return listeners[0];
			return new SingletonListener() {

				@Override
				public <T> void onSingletonCreated(Resource<T> resource,
						T instance) {
					for (SingletonListener l : listeners)
						l.onSingletonCreated(resource, instance);
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
			Initialiser<Injector>[] injectorPostConstruct = resolve(
					injectorInitType.addArrayDimension());
			postConstructByActualType.put(Injector.class,
					injectorPostConstruct);
			if (inits.length == injectorPostConstruct.length)
				return copyOf(inits, 0); // no other dynamic initialisers
			return arrayFilter(inits, c -> !c.type().equalTo(injectorInitType));
		}

		@SuppressWarnings("unchecked")
		private Env defaultEnv(Injectee<?>... injectees) {
			Instance<Env> defaultEnvInstance = Instance.defaultInstanceOf(
					raw(Env.class));
			for (Injectee<?> injectee : injectees) {
				if (injectee.locator.instance.equalTo(defaultEnvInstance))
					return ((Supplier<? extends Env>) injectee.supplier).supply(
							Dependency.dependency(defaultEnvInstance), this);
			}
			return null;
		}

		private Map<Class<?>, Resource<?>[]> createResources(
				Injectee<?>... injectees) {
			Resource<?>[] resources = new Resource<?>[injectees.length];
			Env env = defaultEnv(injectees);
			Annotated.Merge annotationMerger = env == null
				? Annotated.NO_MERGE
				: env.property(Annotated.Merge.class,
						Container.class.getPackage());
			Map<Name, Resource<ScopePermanence>> permanenceResourceByScope = new HashMap<>();
			Map<Name, ScopePermanence> permanenceByScope = new HashMap<>();
			Injector bootrsappingContext = createBootstrappingContext(
					permanenceResourceByScope, permanenceByScope);
			// create ScopePermanence resources
			for (int i = 0; i < injectees.length; i++) {
				if (injectees[i].locator.type().rawType == ScopePermanence.class) {
					Resource<?> r = createScopePermanenceResource(i,
							injectees[i],
							effectiveAnnotated(injectees[i], annotationMerger),
							bootrsappingContext);
					resources[i] = r;
					@SuppressWarnings("unchecked")
					Resource<ScopePermanence> r2 = (Resource<ScopePermanence>) r;
					permanenceResourceByScope.put(r.locator.instance.name, r2);
				}
			}
			// make sure all ScopePermanence are known
			for (Entry<Name, Resource<ScopePermanence>> e : permanenceResourceByScope.entrySet()) {
				if (!permanenceByScope.containsKey(e.getKey())) {
					Resource<ScopePermanence> val = e.getValue();
					permanenceByScope.put(e.getKey(),
							val.generate(val.locator.toDependency()));
				}
			}
			// create rest of resources
			for (int i = 0; i < injectees.length; i++) {
				if (resources[i] == null) {
					resources[i] = createResource(i, injectees[i],
							effectiveAnnotated(injectees[i], annotationMerger),
							permanenceByScope);
				}
			}
			return createResourcesByRawType(resources);
		}

		private static Injector createBootstrappingContext(
				Map<Name, Resource<ScopePermanence>> permanenceResourceByScope,
				Map<Name, ScopePermanence> permanenceByScope) {
			return new Injector() {

				@SuppressWarnings("unchecked")
				@Override
				public <E> E resolve(Dependency<E> dep)
						throws UnresolvableDependency {
					Class<E> rawType = dep.type().rawType;
					if (rawType == Injector.class)
						return (E) this;
					if (rawType == ScopePermanence.class) {
						Name scope = dep.instance.name;
						if (!permanenceResourceByScope.containsKey(scope))
							throw noResourceFor(dep);
						Dependency<ScopePermanence> scopeDep = (Dependency<ScopePermanence>) dep;
						return (E) permanenceByScope.computeIfAbsent(scope,
								name -> permanenceResourceByScope.get(
										name).generator.generate(scopeDep));
					}
					throw noResourceFor(dep);
				}

				private NoResourceForDependency noResourceFor(
						Dependency<?> dep) {
					return new NoResourceForDependency(
							"During bootstrapping only ", dep,
							permanenceResourceByScope.values().toArray(
									new Resource[0]));
				}
			};
		}

		private static Map<Class<?>, Resource<?>[]> createResourcesByRawType(
				Resource<?>[] resources) {
			Arrays.sort(resources);
			Map<Class<?>, Resource<?>[]> byRawType = new IdentityHashMap<>(
					resources.length);
			if (resources.length == 0)
				return byRawType;
			Class<?> lastRawType = resources[0].type().rawType;
			int start = 0;
			for (int i = 0; i < resources.length; i++) {
				Class<?> rawType = resources[i].type().rawType;
				if (rawType != lastRawType) {
					byRawType.put(lastRawType,
							copyOfRange(resources, start, i));
					start = i;
				}
				lastRawType = rawType;
			}
			byRawType.put(lastRawType,
					copyOfRange(resources, start, resources.length));
			return byRawType;
		}

		private static <T> Annotated effectiveAnnotated(Injectee<T> injectee,
				Annotated.Merge merger) {
			return injectee.supplier instanceof Annotated
				? merger.apply((Annotated) injectee.supplier)
				: Annotated.WITH_NO_ANNOTATIONS;
		}

		private <T> Resource<T> createResource(int serialID,
				Injectee<T> injectee, Annotated annotations,
				Map<Name, ScopePermanence> permanenceByScope) {
			// NB. using the function is a way to allow both Resource and Generator implementation to be initialised with a final reference of each other
			Function<Resource<T>, Generator<T>> generatorFactory = //
					resource -> createGenerator(resource, injectee.supplier);
			ScopePermanence scoping = permanenceByScope.get(injectee.scope);
			if (scoping == null)
				throw new InconsistentDeclaration("Scope `" + injectee.scope
					+ "` is used but not defined for: " + injectee);
			return new Resource<>(serialID, injectee.source, scoping,
					injectee.locator, generatorFactory, annotations);
		}

		private static <T> Resource<T> createScopePermanenceResource(
				int serialID, Injectee<T> injectee, Annotated annotations,
				Injector bootstrappingContext) {
			return new Resource<>(serialID, injectee.source,
					ScopePermanence.container, injectee.locator,
					resource -> (dependency -> injectee.supplier.supply(
							dependency, bootstrappingContext)),
					annotations);
		}

		@SuppressWarnings("unchecked")
		private <T> Generator<T> createGenerator(Resource<T> resource,
				Supplier<? extends T> supplier) {
			Name scope = resource.permanence.scope;
			if (Generator.class.isAssignableFrom(supplier.getClass()))
				return (Generator<T>) supplier;
			if (Scope.class.isAssignableFrom(resource.type().rawType)
				|| Scope.container.equalTo(scope))
				return new LazySingletonGenerator<>(this, supplier, resource);
			if (Scope.reference.equalTo(scope))
				return new ReferenceGenerator<>(this, supplier, resource);
			return new LazyScopedGenerator<>(this, resource, supplier);
		}

		private static Resource<?>[] selectGenericResources(
				Map<Class<?>, Resource<?>[]> byRawType) {
			List<Resource<?>> res = new ArrayList<>();
			for (Resource<?>[] forType : byRawType.values())
				for (Resource<?> resource : forType) {
					Type<?> type = resource.type();
					if (type.isUpperBound()
						|| type.isParameterizedAsUpperBound()) //TODO this should not be needed as this should match by raw type list
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
				return (T) decorated;
			if (rawType == Env.class && dep.instance.name.equalTo(Name.AS))
				return (T) this;
			return resolveFromResource(dep, type, rawType);
		}

		@SuppressWarnings("unchecked")
		private <T> T resolveFromResource(Dependency<T> dep, final Type<T> type,
				final Class<T> rawType) {
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

		/**
		 * Can be called by a {@link Generator} to create an instance from a
		 * {@link Supplier} and have {@link Initialiser}s applied for it as well
		 * as notifying {@link SingletonListener}s.
		 */
		<T> T createInstance(Dependency<? super T> injected,
				Supplier<? extends T> supplier, Resource<T> resource) {
			T instance = supplier.supply(injected, getDecorated());
			if (instance != null && postConstruct != null
				&& postConstruct.length > 0)
				instance = postConstruct(instance, injected);
			if (resource.permanence.isPermanent()
				&& singletonListener != null) {
				singletonListener.onSingletonCreated(resource, instance);
			}
			return instance;
		}

		@SuppressWarnings("unchecked")
		private <T> T postConstruct(T instance, Dependency<?> context) {
			Class<T> actualType = (Class<T>) instance.getClass();
			if (actualType == Class.class || actualType == Resource.class
				|| Initialiser.class.isAssignableFrom(actualType)) {
				return instance;
			}
			Initialiser<? super T>[] cachedPostConstructs = (Initialiser<? super T>[]) //
			postConstructByActualType.computeIfAbsent(actualType,
					key -> arrayFlatmap(postConstruct, Initialiser.class,
							rx -> matchAndGenerateInitialiser(actualType, rx,
									context)));
			if (cachedPostConstructs.length > 0)
				for (Initialiser<? super T> ix : cachedPostConstructs)
					instance = (T) ix.init(instance, getDecorated());
			return instance;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		private static <T, I extends Initialiser<?>> Initialiser<? super T> matchAndGenerateInitialiser(
				Class<T> actualType, Resource<I> resource,
				Dependency<?> context) {
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
			I instance = resource.generate(dependency(initialiser.instance));
			if (instance instanceof Predicate
				&& Type.classType(instance.getClass()).isAssignableTo(
						raw(Predicate.class).parametized(Class.class))) {
				Predicate<Class<?>> filter = (Predicate<Class<?>>) instance;
				if (!filter.test(actualType))
					return null;
			}
			return (Initialiser<? super T>) instance;
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
				b.append(rx.permanence).append(' ');
				b.append(rx.source).append('\n');
			}
		}
	}

	/**
	 * This {@link Generator} represents the {@link Scope#container} where the
	 * {@link LazySingletonGenerator#value} field holds the singleton value.
	 * 
	 * In contrast to other scopes that are implemented as instances of
	 * {@link Scope} the {@link Scope#container} is "virtual". Its instances
	 * exist in the different instances of the {@link LazyScopedGenerator}.
	 * 
	 * This is required to bootstrap {@link Scope} instances that cannot
	 * dependent on themselves to already exist but it is also used to simplify
	 * the "generation" for known constants (instances that are already
	 * created).
	 * 
	 * The {@link Lazy} value makes sure the {@link Supplier} is only ever
	 * called once.
	 * 
	 * @param <T> Type of the lazy value
	 */
	private static final class LazySingletonGenerator<T>
			implements Generator<T> {

		private final InjectorImpl injector;
		private final Supplier<? extends T> supplier;
		private final Resource<T> resource;
		private final Lazy<T> value = new Lazy<>();

		LazySingletonGenerator(InjectorImpl injector,
				Supplier<? extends T> supplier, Resource<T> resource) {
			this.injector = injector;
			this.supplier = supplier;
			this.resource = resource;
		}

		@Override
		public T generate(Dependency<? super T> dep)
				throws UnresolvableDependency {
			dep.ensureNoIllegalDirectAccessOf(resource.locator);
			return value.get(() -> injector.createInstance(
					dep.injectingInto(resource.locator, resource.permanence),
					supplier, resource));
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
		private final Resource<T> resource;

		ReferenceGenerator(Injector injector, Supplier<? extends T> supplier,
				Resource<T> resource) {
			this.injector = injector;
			this.supplier = supplier;
			this.resource = resource;
		}

		@Override
		public T generate(Dependency<? super T> dep)
				throws UnresolvableDependency {
			dep.ensureNoIllegalDirectAccessOf(resource.locator);
			return supplier.supply(
					dep.injectingInto(resource.locator, ScopePermanence.ignore),
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
		private final Supplier<? extends T> supplier;
		private final Lazy<Scope> scope = new Lazy<>();
		private final Resource<T> resource;

		LazyScopedGenerator(InjectorImpl injector, Resource<T> resource,
				Supplier<? extends T> supplier) {
			this.resource = resource;
			this.injector = injector;
			this.supplier = supplier;
		}

		private Scope resolveScope() {
			return injector.resolve(resource.permanence.scope, Scope.class);
		}

		@Override
		public T generate(Dependency<? super T> dep) {
			dep.ensureNoIllegalDirectAccessOf(resource.locator);
			final Dependency<? super T> injected = dep.injectingInto(
					resource.locator, resource.permanence);
			/**
			 * This cache makes sure that within one thread even if the provider
			 * (lambda below) is called multiple times (which can occur because
			 * methods like {@code updateAndGet} on atomics have a loop) will
			 * always yield the same instance. Different invocation of this
			 * method (yield) however can lead to multiple calls to the
			 * supplier.
			 */
			AtomicReference<T> instanceCache = new AtomicReference<>();
			return scope.get(this::resolveScope).provide(resource.serialID,
					injected, () -> // 
					instanceCache.updateAndGet(instance -> instance != null
						? instance
						: createInstance(injected)),
					injector.generators);
		}

		private T createInstance(Dependency<? super T> injected) {
			return injector.createInstance(injected, supplier, resource);
		}

	}
}
