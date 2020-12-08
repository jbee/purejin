package se.jbee.inject.container;

import static java.util.Arrays.copyOfRange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import se.jbee.inject.ContextAware;
import se.jbee.inject.Dependency;
import se.jbee.inject.Generator;
import se.jbee.inject.InconsistentDeclaration;
import se.jbee.inject.Injector;
import se.jbee.inject.Locator;
import se.jbee.inject.Name;
import se.jbee.inject.Provider;
import se.jbee.inject.Resource;
import se.jbee.inject.ResourceDescriptor;
import se.jbee.inject.Scope;
import se.jbee.inject.ScopePermanence;
import se.jbee.inject.Supplier;
import se.jbee.inject.lang.Type;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;

/**
 * A set of {@link Resources} encapsulates the state and bootstrapping of
 * {@link Resource}s from {@link ResourceDescriptor}s.
 *
 * As {@link Resource}s operate within an {@link Injector} context they need
 * some way of delegating actual instantiation (or supply) of generated
 * instances back to the {@link Injector} context which they are a part of so
 * that the context can do further processing. This backwards link is supplied
 * as the {@link ResourceLink}.
 *
 * @since 8.1
 */
final class Resources {

	private final int resourceCount;
	private final Map<Class<?>, Resource<?>[]> resourcesByType;
	private final Resource<?>[] resources;
	private final Resource<?>[] genericResources;

	/**
	 * Creates a set of grouped {@link Resource} from
	 * {@link ResourceDescriptor}s.
	 *
	 * @param link backlink to the internals of the {@link Injector} context
	 *            this resources is created for which is provided by the
	 *            {@link Injector} implementation
	 * @param scopes function to lookup (yield) {@link Scope} by {@link Name}
	 *            (also provided by the created {@link Injector} context)
	 * @param descriptors the list of {@link ResourceDescriptor}s that
	 *            {@link Resource}s are created for. Note that this list must be
	 *            sorted already from the most qualified to the least qualified
	 *            for each raw type. The order of the raw type groups is
	 *            irrelevant.
	 */
	Resources(ResourceLink link, Function<Name, Scope> scopes,
			ResourceDescriptor<?>... descriptors) {
		this.resourceCount = descriptors.length;
		this.resources = createResources(link, scopes, descriptors);
		this.resourcesByType = createResourcesByRawType(resources);
		this.genericResources = selectGenericResources(resourcesByType);
	}

	@SuppressWarnings("unchecked")
	public <T> Resource<T>[] forType(Type<T> type) {
		if (type.equalTo(Type.WILDCARD))
			return (Resource<T>[]) genericResources;
		return (Resource<T>[]) resourcesByType.get(type.rawType);
	}

	public Set<Entry<Class<?>, Resource<?>[]>> entrySet() {
		return resourcesByType.entrySet();
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
			Locator<?> r = rx.signature;
			b.append("\t#").append(rx.serialID).append(' ');
			b.append(r.type().simpleName()).append(' ');
			b.append(r.instance.name).append(' ');
			b.append(r.target).append(' ');
			b.append(rx.permanence).append(' ');
			b.append(rx.source).append('\n');
		}
	}

	public void initEager() {
		for (Resource<?> eager : resources)
			eager.init();
	}

	private static Resource<?>[] selectGenericResources(
			Map<Class<?>, Resource<?>[]> byRawType) {
		List<Resource<?>> res = new ArrayList<>();
		for (Resource<?>[] forType : byRawType.values())
			for (Resource<?> resource : forType) {
				Type<?> type = resource.type();
				if (type.isUpperBound() || type.isParameterizedAsUpperBound()) //TODO this should not be needed as this should match by raw type list
					res.add(resource);
			}
		Collections.sort(res);
		return res.isEmpty() ? null : res.toArray(new Resource[0]);
	}

	private Resource<?>[] createResources(ResourceLink link,
			Function<Name, Scope> scopes, ResourceDescriptor<?>[] descriptors) {
		Resource<?>[] res = new Resource<?>[descriptors.length];

		Map<Name, Resource<ScopePermanence>> permanenceResourceByScope = new HashMap<>();
		Map<Name, ScopePermanence> permanenceByScope = new HashMap<>();
		Injector bootstrappingContext = createBootstrappingContext(
				permanenceResourceByScope, permanenceByScope);
		// create ScopePermanence resources
		for (int i = 0; i < descriptors.length; i++) {
			ResourceDescriptor<?> descriptor = descriptors[i];
			if (descriptor.signature.type().rawType == ScopePermanence.class) {
				Resource<?> r = createScopePermanenceResource(i, descriptor,
						bootstrappingContext);
				res[i] = r;
				@SuppressWarnings("unchecked")
				Resource<ScopePermanence> r2 = (Resource<ScopePermanence>) r;
				permanenceResourceByScope.put(r.signature.instance.name, r2);
			}
		}
		// make sure all ScopePermanence are known
		for (Entry<Name, Resource<ScopePermanence>> e : permanenceResourceByScope.entrySet()) {
			if (!permanenceByScope.containsKey(e.getKey())) {
				Resource<ScopePermanence> val = e.getValue();
				permanenceByScope.put(e.getKey(),
						val.generate(val.signature.toDependency()));
			}
		}
		// create rest of resources
		for (int i = 0; i < descriptors.length; i++)
			if (res[i] == null)
				res[i] = createResource(link, scopes, i, descriptors[i],
						permanenceByScope);
		return res;
	}

	private static Injector createBootstrappingContext(
			Map<Name, Resource<ScopePermanence>> permanenceResourceByScope,
			Map<Name, ScopePermanence> permanenceByScope) {
		return new Injector() {

			@SuppressWarnings({ "unchecked", "ChainOfInstanceofChecks" })
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

			private NoResourceForDependency noResourceFor(Dependency<?> dep) {
				return new NoResourceForDependency("During bootstrapping only ",
						dep, permanenceResourceByScope.values().toArray(
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
				byRawType.put(lastRawType, copyOfRange(resources, start, i));
				start = i;
			}
			lastRawType = rawType;
		}
		byRawType.put(lastRawType,
				copyOfRange(resources, start, resources.length));
		return byRawType;
	}

	private <T> Resource<T> createResource(ResourceLink link,
			Function<Name, Scope> scopes, int serialID,
			ResourceDescriptor<T> descriptor,
			Map<Name, ScopePermanence> permanenceByScope) {
		// NB. using the function is a way to allow both Resource and Generator implementation to be initialised with a final reference of each other
		Function<Resource<T>, Generator<T>> generatorFactory = //
				resource -> createGenerator(link, scopes, resource,
						descriptor.supplier);
		ScopePermanence scoping = permanenceByScope.get(descriptor.scope);
		if (scoping == null)
			throw new InconsistentDeclaration("Scope `" + descriptor.scope
				+ "` is used but not defined for: " + descriptor);
		return new Resource<>(serialID, descriptor.source, scoping,
				descriptor.signature, descriptor.annotations,
				descriptor.verifier, generatorFactory);
	}

	private static <T> Resource<T> createScopePermanenceResource(int serialID,
			ResourceDescriptor<T> descriptor, Injector bootstrappingContext) {
		return new Resource<>(serialID, descriptor.source,
				ScopePermanence.container, descriptor.signature,
				descriptor.annotations, descriptor.verifier,
					resource -> (dependency ->	descriptor.supplier
							.supply(dependency, bootstrappingContext)));
	}

	@SuppressWarnings("unchecked")
	private <T> Generator<T> createGenerator(ResourceLink link,
			Function<Name, Scope> scopes, Resource<T> resource,
			Supplier<? extends T> supplier) {
		if (supplier.isGenerator())
			return (Generator<T>) supplier.asGenerator();
		Name scope = resource.permanence.scope;
		Generator<T> inContext = dep -> link.supplyInContext(dep, supplier,
				resource);
		if (Scope.class.isAssignableFrom(resource.type().rawType)
			|| Scope.container.equalTo(scope))
			return new LazySingletonGenerator<>(inContext, resource);
		if (Scope.reference.equalTo(scope))
			return new ReferenceGenerator<>(inContext, resource);
		// default is a scoped generator...
		return new LazyScopedGenerator<>(inContext, resource, resourceCount,
				() -> scopes.apply(resource.permanence.scope));
	}

	public void verifyIn(Injector context) {
		for (Resource<?> r : resources)
			r.verifier.verifyIn(context);
	}

	/**
	 * This {@link Generator} represents the {@link Scope#container} where the
	 * {@link LazySingletonGenerator#value} field holds the singleton value.
	 *
	 * In contrast to other scopes that are implemented as instances of
	 * {@link Scope} the {@link Scope#container} is "virtual". Its instances
	 * exist in the different instances of the {@link LazySingletonGenerator}.
	 *
	 * This is required to bootstrap {@link Scope} instances that cannot
	 * dependent on themselves to already exist but it is also used to simplify
	 * the "generation" for known constants (instances that are already
	 * created).
	 *
	 * The {@link Lazy} {@link #value} makes sure the {@link #inContext}
	 * {@link Generator} is only ever called once to actually yield a fresh
	 * instance.
	 *
	 * @param <T> Type of the lazy value generated
	 */
	private static final class LazySingletonGenerator<T>
			implements Generator<T> {

		private final Generator<T> inContext;
		private final Resource<T> resource;
		private final Lazy<T> value = new Lazy<>();

		LazySingletonGenerator(Generator<T> inContext, Resource<T> resource) {
			this.inContext = inContext;
			this.resource = resource;
		}

		@Override
		public T generate(Dependency<? super T> dep)
				throws UnresolvableDependency {
			dep.ensureNoIllegalDirectAccessOf(resource.signature);
			return value.get(() -> provide(dep));
		}

		private T provide(Dependency<? super T> dep) {
			return inContext.generate(
					dep.injectingInto(resource.signature, resource.permanence));
		}
	}

	/**
	 * Special {@link Generator} for forward referencing {@link Resource}s.
	 * These are created with {@link Scope#reference}.
	 *
	 * @param <T> Type of the generated value
	 */
	private static final class ReferenceGenerator<T> implements Generator<T> {

		private final Generator<T> inContext;
		private final Resource<T> resource;

		ReferenceGenerator(Generator<T> inContext, Resource<T> resource) {
			this.inContext = inContext;
			this.resource = resource;
		}

		@Override
		public T generate(Dependency<? super T> dep)
				throws UnresolvableDependency {
			dep.ensureNoIllegalDirectAccessOf(resource.signature);
			return inContext.generate(dep.injectingInto(resource.signature,
					ScopePermanence.ignore));
		}
	}

	/**
	 * Default {@link Generator} that uses a {@link Scope} implementation to
	 * manage the value.
	 *
	 * @param <T> Type of the generated value
	 */
	private static final class LazyScopedGenerator<T> implements Generator<T> {

		private final Generator<T> inContext;
		private final Lazy<Scope> scope = new Lazy<>();
		private final Resource<T> resource;
		private final int resources;
		private final Provider<Scope> scopeProvider;

		LazyScopedGenerator(Generator<T> inContext, Resource<T> resource,
				int resources, Provider<Scope> scope) {
			this.resource = resource;
			this.inContext = inContext;
			this.resources = resources;
			this.scopeProvider = scope;
		}

		@Override
		public T generate(Dependency<? super T> dep) {
			dep.ensureNoIllegalDirectAccessOf(resource.signature);
			final Dependency<? super T> injected = dep.injectingInto(
					resource.signature, resource.permanence);
			/*
			 * This cache makes sure that within one thread even if the provider
			 * (lambda below) is called multiple times (which can occur because
			 * methods like {@code updateAndGet} on atomics have a loop) will
			 * always yield the same instance. Different invocation of this
			 * method (yield) however can lead to multiple calls to the
			 * supplier.
			 */
			AtomicReference<T> instanceCache = new AtomicReference<>();
			T res = scope.get(scopeProvider).provide(resource.serialID,
					resources, injected,
					() -> instanceCache.updateAndGet(
							instance -> instance != null
								? instance
								: inContext.generate(injected)));
			if (res instanceof ContextAware) {
				@SuppressWarnings("unchecked")
				ContextAware<T> contextAware = (ContextAware<T>) res;
				return contextAware.inContext(dep);
			}
			return res;
		}

	}
}
