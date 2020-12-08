package se.jbee.inject.container;

import se.jbee.inject.*;
import se.jbee.inject.lang.Type;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.copyOf;
import static se.jbee.inject.Cast.initialiserTypeOf;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.lang.Type.raw;
import static se.jbee.inject.lang.Utils.arrayFilter;
import static se.jbee.inject.lang.Utils.arrayMap;

/**
 * A {@link PostConstruct} encapsulates the state and processing of
 * {@link Initialiser}s within an {@link Injector} context which perform typical
 * "post construct" tasks by altering or replacing the freshly generated
 * instance before it exists the instance resolution of the {@link Injector}.
 */
public final class PostConstruct {

	private final Initialiser.Sorter sorter;
	private final Resource<? extends Initialiser<?>>[] resources;
	private final Map<Class<?>, Map<Resource<?>, Initialiser<?>>> byTargetRawType = new ConcurrentHashMap<>();

	public PostConstruct(Initialiser.Sorter sorter,
			Resource<? extends Initialiser<?>>[] resources) {
		this.sorter = sorter;
		this.resources = resources;
	}

	public Injector postConstruct(Injector container) {
		return postConstruct(container, dependency(Injector.class), container);
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
		return applyPostConstruct(instance, injected, context,
				byTargetRawType.computeIfAbsent(actualType,
						key -> matchingInits(injected, key)));
	}

	/**
	 * The implementations makes some gymnastics to allow sorting the {@link
	 * Initialiser}s as array while internally we need the corresponding {@link
	 * Resource} as well.
	 */
	private Map<Resource<?>, Initialiser<?>> matchingInits(Dependency<?> injected,
			Class<?> actualType) {
		Map<Initialiser<?>, Resource<?>> matching = new IdentityHashMap<>(); // OBS! important we use identity as key
		for (Resource<? extends Initialiser<?>> r : resources) {
			Initialiser<?> init = createInit(actualType, r, injected);
			if (init != null)
				matching.put(init, r);
		}
		Initialiser[] unsorted = matching.keySet().toArray(new Initialiser[0]);
		Initialiser<?>[] sorted = unsorted.length <= 1
				? unsorted
				: sorter.sort(actualType, unsorted);
		Map<Resource<?>, Initialiser<?>> res = new LinkedHashMap<>();
		for (Initialiser<?> init : sorted)
			res.put(matching.get(init), init);
		return res;
	}

	@SuppressWarnings("unchecked")
	private static <T> T applyPostConstruct(T instance, Dependency<?> injected,
			Injector context, Map<Resource<?>, Initialiser<?>> inits) {
		if (inits != null && !inits.isEmpty())
			for (Map.Entry<Resource<?>, Initialiser<?>> init : inits.entrySet()) {
				Target target = init.getKey().signature.target;
				if (target.isAvailableFor(injected))
					instance = (T) ((Initialiser<? super T>) init.getValue()) //
						.init(instance, context);
			}
		return instance;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <T, I extends Initialiser<?>> Initialiser<? super T> createInit(
			Class<T> actualType, Resource<I> resource, Dependency<?> context) {
		Locator<I> initialiser = resource.signature;
		if (!initialiser.target.isAccessibleFor(context)) //OBS! here we only check general accessability as other parts need to be checked dynamically
			return null;
		Type<?> required = initialiser.type().parameter(0);
		if (!raw(actualType).isAssignableTo(required))
			return null;
		Type<?> provided = Type.supertype(required.rawType,
				(Type) Type.classType(actualType));
		if (!provided.isAssignableTo(required))
			return null;
		I init = resource.generate();
		if (!matchesInitFilter(init, context.instance, actualType))
			return null;
		return (Initialiser<? super T>) init;
	}

	private static <T, I extends Initialiser<?>> boolean matchesInitFilter(
			I init, Instance<?> contract, Class<T> actualType) {
		if (init.isFiltered())
			return init.asFilter().test(actualType);
		return true;
	}
}
