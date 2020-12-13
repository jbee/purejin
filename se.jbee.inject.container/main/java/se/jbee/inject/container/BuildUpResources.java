package se.jbee.inject.container;

import se.jbee.inject.*;
import se.jbee.inject.lang.Type;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.lang.Type.raw;

/**
 * A {@link BuildUpResources} encapsulates the state and processing of
 * {@link BuildUp}s within an {@link Injector} context which perform typical
 * "post construct" tasks by altering or replacing the freshly generated
 * instance before it exists the instance resolution of the {@link Injector}.
 */
public final class BuildUpResources {

	private final BuildUp.Sequencer sequencer;
	private final Resource<? extends BuildUp<?>>[] resources;
	/**
	 * Again we cannot use {@link java.util.concurrent.ConcurrentHashMap} as the
	 * recursive nature of dependency resolution could lead to reverse
	 * modification which that class does not allow so once more {@link
	 * ConcurrentSkipListMap} to the rescue.
	 */
	private final Map<Class<?>, Map<Resource<?>, BuildUp<?>>> byTargetRawType = new ConcurrentSkipListMap<>(
			Comparator.comparing(Class::getName));

	public BuildUpResources(BuildUp.Sequencer sequencer,
			Resource<? extends BuildUp<?>>[] resources) {
		this.sequencer = sequencer;
		this.resources = resources;
	}

	public Injector buildUp(Injector container) {
		return buildUp(container, dependency(Injector.class), container);
	}

	@SuppressWarnings("unchecked")
	public <T> T buildUp(T instance, Dependency<? super T> injected,
			Injector context) {
		if (resources.length == 0)
			return instance;
		Class<T> actualType = (Class<T>) instance.getClass();
		if (actualType == Class.class
				|| actualType == Resource.class
				|| BuildUp.class.isAssignableFrom(actualType)) {
			return instance;
		}
		return applyBuildUps(instance, injected, context,
				byTargetRawType.computeIfAbsent(actualType,
						key -> findMatchingBuildUps(injected, key)));
	}

	/**
	 * The implementations makes some gymnastics to allow sorting the {@link
	 * BuildUp}s as array while internally we need the corresponding {@link
	 * Resource} as well.
	 */
	private Map<Resource<?>, BuildUp<?>> findMatchingBuildUps(
			Dependency<?> injected, Class<?> actualType) {
		Map<BuildUp<?>, Resource<?>> matching = new IdentityHashMap<>(); // OBS! important we use identity as key
		for (Resource<? extends BuildUp<?>> r : resources) {
			BuildUp<?> buildUp = generateBuildUp(actualType, r, injected);
			if (buildUp != null)
				matching.put(buildUp, r);
		}
		BuildUp<?>[] unsorted = matching.keySet().toArray(new BuildUp[0]);
		BuildUp<?>[] sorted = unsorted.length <= 1
				? unsorted
				: sequencer.order(actualType, unsorted);
		Map<Resource<?>, BuildUp<?>> res = new LinkedHashMap<>();
		for (BuildUp<?> buildUp : sorted)
			res.put(matching.get(buildUp), buildUp);
		return res;
	}

	@SuppressWarnings("unchecked")
	private static <T> T applyBuildUps(T instance,
			Dependency<? super T> injected, Injector context,
			Map<Resource<?>, BuildUp<?>> buildUps) {
		if (buildUps != null && !buildUps.isEmpty()) {
			Type<? super T> type = injected.type();
			for (Map.Entry<Resource<?>, BuildUp<?>> buildUp : buildUps.entrySet()) {
				Target target = buildUp.getKey().signature.target;
				if (target.isAvailableFor(injected))
					instance = (T) ((BuildUp<? super T>) buildUp.getValue()) //
							.buildUp(instance, type, context);
			}
		}
		return instance;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <T, B extends BuildUp<?>> BuildUp<? super T> generateBuildUp(
			Class<T> actualType, Resource<B> resource, Dependency<?> context) {
		Locator<B> signature = resource.signature;
		if (!signature.target.isAccessibleFor(context)) //OBS! here we only check general accessability as other parts need to be checked dynamically
			return null;
		Type<?> required = signature.type().parameter(0);
		if (!raw(actualType).isAssignableTo(required))
			return null;
		Type<?> provided = Type.supertype(required.rawType,
				(Type) Type.classType(actualType));
		if (!provided.isAssignableTo(required))
			return null;
		B buildUpInstance = resource.generate();
		if (!matchesFilter(buildUpInstance, actualType))
			return null;
		return (BuildUp<? super T>) buildUpInstance;
	}

	private static <T, B extends BuildUp<?>> boolean matchesFilter(B buildUp,
			Class<T> actualType) {
		if (buildUp.isFiltered())
			return buildUp.asFilter().test(actualType);
		return true;
	}
}
