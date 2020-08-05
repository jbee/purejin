package se.jbee.inject.container;

import static java.util.Arrays.copyOf;
import static se.jbee.inject.Cast.initialiserTypeOf;
import static se.jbee.inject.lang.Type.raw;
import static se.jbee.inject.lang.Utils.arrayFilter;
import static se.jbee.inject.lang.Utils.arrayFlatmap;
import static se.jbee.inject.lang.Utils.arrayMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import se.jbee.inject.Dependency;
import se.jbee.inject.Initialiser;
import se.jbee.inject.Injector;
import se.jbee.inject.Locator;
import se.jbee.inject.Resource;
import se.jbee.inject.lang.Type;

/**
 * A {@link PostConstruct} encapsulates the state and processing of
 * {@link Initialiser}s within an {@link Injector} context which perform typical
 * "post construct" tasks by altering or replacing the freshly generated
 * instance before it exists the instance resolution of the {@link Injector}.
 */
public final class PostConstruct {

	private final Initialiser.Sorter sorter;
	private final Resource<? extends Initialiser<?>>[] resources;
	private final Map<Class<?>, Initialiser<?>[]> byTargetRawType = new ConcurrentHashMap<>();

	public PostConstruct(Initialiser.Sorter sorter,
			Resource<? extends Initialiser<?>>[] inits) {
		this.sorter = sorter;
		this.resources = withoutInjectorResources(inits);
	}

	private Resource<? extends Initialiser<?>>[] withoutInjectorResources(
			Resource<? extends Initialiser<?>>[] inits) {
		if (inits.length == 0)
			return inits;
		Type<Initialiser<Injector>> injectorInitType = initialiserTypeOf(
				Injector.class);
		Initialiser<?>[] injectorInits = sorter.sort(Injector.class,
				arrayMap(arrayFilter(inits, //
						e -> e.type().equalTo(injectorInitType)),
						Initialiser.class, Resource::generate));
		byTargetRawType.put(Injector.class, injectorInits);
		if (inits.length == injectorInits.length)
			return copyOf(inits, 0); // no other dynamic initialisers
		return arrayFilter(inits, c -> !c.type().equalTo(injectorInitType));
	}

	public Injector postConstruct(Injector container) {
		return applyPostConstruct(container, container,
				byTargetRawType.get(Injector.class));
	}

	@SuppressWarnings("unchecked")
	public <T> T postConstruct(T instance, Dependency<?> injected,
			Injector context) {
		if (resources.length == 0)
			return instance;
		Class<T> actualType = (Class<T>) instance.getClass();
		if (actualType == Class.class || actualType == Resource.class
			|| Initialiser.class.isAssignableFrom(actualType)) {
			return instance;
		}
		return applyPostConstruct(instance, context,
				byTargetRawType.computeIfAbsent(actualType,
						key -> matchingInits(injected, key)));
	}

	private Initialiser<?>[] matchingInits(Dependency<?> injected,
			Class<?> actualType) {
		return sorter.sort(actualType, arrayFlatmap(resources,
				Initialiser.class, rx -> createInit(actualType, rx, injected)));
	}

	@SuppressWarnings("unchecked")
	private static <T> T applyPostConstruct(T instance, Injector context,
			Initialiser<?>[] inits) {
		if (inits != null && inits.length > 0)
			for (Initialiser<?> ix : inits)
				instance = (T) ((Initialiser<? super T>) ix).init(instance,
						context);
		return instance;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <T, I extends Initialiser<?>> Initialiser<? super T> createInit(
			Class<T> actualType, Resource<I> resource, Dependency<?> context) {
		Locator<I> initialiser = resource.signature;
		if (!initialiser.target.isAvailableFor(context))
			return null;
		Type<?> required = initialiser.type().parameter(0);
		if (!raw(actualType).isAssignableTo(required))
			return null;
		Type<?> provided = Type.supertype(required.rawType,
				(Type) Type.classType(actualType));
		if (!provided.isAssignableTo(required))
			return null;
		I instance = resource.generate();
		if (!matchesInitFilter(instance, actualType))
			return null;
		return (Initialiser<? super T>) instance;
	}

	private static <T, I extends Initialiser<?>> boolean matchesInitFilter(
			I instance, Class<T> actualType) {
		if (instance instanceof Predicate
			&& Type.classType(instance.getClass()).isAssignableTo(
					raw(Predicate.class).parametized(Class.class))) {
			@SuppressWarnings("unchecked")
			Predicate<Class<?>> filter = (Predicate<Class<?>>) instance;
			return filter.test(actualType);
		}
		return true; // does not have a filter so it matches
	}
}
