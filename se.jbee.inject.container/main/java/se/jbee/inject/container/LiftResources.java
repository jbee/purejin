package se.jbee.inject.container;

import se.jbee.inject.*;
import se.jbee.lang.Type;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import static se.jbee.inject.Dependency.dependency;
import static se.jbee.lang.Type.classType;
import static se.jbee.lang.Type.raw;

/**
 * A {@link LiftResources} encapsulates the state and processing of
 * {@link Lift}s within an {@link Injector} context which perform typical
 * "post construct" tasks by altering or replacing the freshly generated
 * instance before it exists the instance resolution of the {@link Injector}.
 */
public final class LiftResources {

	private final Lift.Sequencer sequencer;
	private final Resource<? extends Lift<?>>[] resources;
	/**
	 * Again we cannot use {@link java.util.concurrent.ConcurrentHashMap} as the
	 * recursive nature of dependency resolution could lead to reverse
	 * modification which that class does not allow so once more {@link
	 * ConcurrentSkipListMap} to the rescue.
	 */
	private final Map<Class<?>, Map<Resource<?>, Lift<?>>> byTargetRawType = new ConcurrentSkipListMap<>(
			Comparator.comparing(Class::getName));

	public LiftResources(Lift.Sequencer sequencer,
			Resource<? extends Lift<?>>[] resources) {
		this.sequencer = sequencer;
		this.resources = resources;
	}

	public Injector lift(Injector container) {
		return lift(container, dependency(Injector.class), container);
	}

	@SuppressWarnings("unchecked")
	public <T> T lift(T instance, Dependency<? super T> injected,
			Injector context) {
		if (resources.length == 0)
			return instance;
		Class<T> actualType = (Class<T>) instance.getClass();
		if (actualType == Class.class
				|| actualType == Resource.class
				|| Lift.class.isAssignableFrom(actualType)) {
			return instance;
		}
		return applyLifts(instance, injected, context,
				byTargetRawType.computeIfAbsent(actualType,
						key -> findMatchingLifts(injected, key)));
	}

	/**
	 * The implementations makes some gymnastics to allow sorting the {@link
	 * Lift}s as array while internally we need the corresponding {@link
	 * Resource} as well.
	 */
	private Map<Resource<?>, Lift<?>> findMatchingLifts(
			Dependency<?> injected, Class<?> actualType) {
		Map<Lift<?>, Resource<?>> matching = new IdentityHashMap<>(); // OBS! important we use identity as key
		for (Resource<? extends Lift<?>> r : resources) {
			Lift<?> lift = generateLift(actualType, r, injected);
			if (lift != null)
				matching.put(lift, r);
		}
		Lift<?>[] unsorted = matching.keySet().toArray(Lift[]::new);
		Lift<?>[] sorted = unsorted.length <= 1
				? unsorted
				: sequencer.order(actualType, unsorted);
		Map<Resource<?>, Lift<?>> res = new LinkedHashMap<>();
		for (Lift<?> lift : sorted)
			res.put(matching.get(lift), lift);
		return res;
	}

	@SuppressWarnings("unchecked")
	private static <T> T applyLifts(T instance,
			Dependency<? super T> injected, Injector context,
			Map<Resource<?>, Lift<?>> lifts) {
		if (lifts != null && !lifts.isEmpty()) {
			Type<? super T> type = injected.type();
			for (Map.Entry<Resource<?>, Lift<?>> lift : lifts.entrySet()) {
				Target target = lift.getKey().signature.target;
				if (target.isUsableFor(injected))
					instance = (T) ((Lift<? super T>) lift.getValue()) //
							.lift(instance, type, context);
			}
		}
		return instance;
	}

	@SuppressWarnings("unchecked")
	private static <T, L extends Lift<?>> Lift<? super T> generateLift(
			Class<T> actualType, Resource<L> resource, Dependency<?> context) {
		Locator<L> signature = resource.signature;
		if (!signature.target.isUsablePackageWise(context)) //OBS! here we only check general accessibility as other parts need to be checked dynamically
			return null;
		Type<?> required = signature.type().parameter(0);
		if (!raw(actualType).isAssignableTo(required))
			return null;
		Type<?> provided = classType(actualType).toSuperType(required.rawType);
		if (!provided.isAssignableTo(required))
			return null;
		L liftInstance = resource.generate();
		if (!matchesFilter(liftInstance, actualType))
			return null;
		return (Lift<? super T>) liftInstance;
	}

	private static <T, L extends Lift<?>> boolean matchesFilter(L lift,
			Class<T> actualType) {
		if (lift.isFiltered())
			return lift.asFilter().test(actualType);
		return true;
	}
}
