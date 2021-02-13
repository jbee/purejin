package se.jbee.inject.config;

import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.Lift;
import se.jbee.lang.Type;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static java.util.Arrays.asList;

/**
 * {@link PublishesBy} is a strategy to decide which of the types an actual
 * value type is assignable to should also operate as its API. That means the
 * value or implementation is bound to that API type.
 * <p>
 * Using a strategy to decide the bound API types is most of all a convenience
 * tool that allows to avoid or shorten declarations and to reduce stating a
 * concept or strategy in multiple places in form of their current
 * manifestation.
 * <p>
 * For example, a strategy could be to only access bound types using interfaces.
 * The {@link #PROTECTIVE} strategy could be used to easily do this.
 */
@FunctionalInterface
public interface PublishesBy {

	/**
	 * Whether or not the implementation type is published as the api type
	 * (which is the implementation type itself or one of its supertypes).
	 *
	 * @param api  the raw class type of a super-class or super-interface of the
	 *             provided implementation type
	 * @param impl the implementation type that fulfills the api
	 * @return True, if the given api type should be considered a contract (for
	 * the given implementation type), else false
	 */
	boolean isPublishedAs(Class<?> api, Class<?> impl);

	/**
	 * No API is accepted. Useful to build upon when e.g. using only a {@link
	 * #declaredSet(Set)}.
	 */
	PublishesBy NONE = (api, impl) -> false;

	/**
	 * All APIs are accepted which is the same as binding any and all of the
	 * super-types of a type.
	 */
	PublishesBy SUPER = (api, impl) -> true;

	/**
	 * Binds only APIs that are interfaces unless the implementation does not
	 * implement any directly (it might still inherit some).
	 */
	PublishesBy PROTECTIVE = (api, impl) -> api.isInterface() || api == impl && impl.getInterfaces().length == 0;

	/**
	 * Binds all APIs that are interface types <b>and</b> the exact
	 * implementation class itself.
	 */
	PublishesBy OPTIMISTIC = (api, impl) -> api.isInterface() || api == impl;

	/**
	 * Note that this optimizes and can also return this or the other {@link
	 * PublishesBy} strategy in case an OR would result in same logic anyhow
	 * since {@link #NONE} or {@link #SUPER} is involved.
	 *
	 * @param other another, alternative strategy
	 * @return a {@link PublishesBy} strategy that considers an API a contract
	 * if it either matches this condition or the provided other
	 */
	default PublishesBy or(PublishesBy other) {
		if (this == SUPER || other == NONE) return this;
		if (this == NONE) return other;
		return (api, impl) -> isPublishedAs(api, impl) || other.isPublishedAs(api, impl);
	}

	default PublishesBy and(PublishesBy other) {
		if (other == SUPER || this == NONE) return this;
		if (this == SUPER || other == NONE) return other;
		return (api, impl) -> isPublishedAs(api, impl) && other.isPublishedAs(api, impl);
	}

	default PublishesBy annotatedWith(Class<? extends Annotation> apiAnnotation) {
		return (api, impl) -> api.isAnnotationPresent(apiAnnotation) //
				&& isPublishedAs(api, impl);
	}

	default <T extends Annotation> PublishesBy annotatedWith(
			Class<T> implAnnotation, Function<T, Class<?>[]> apis) {
		return (api, impl) -> impl.isAnnotationPresent(implAnnotation) && asList(
				apis.apply(impl.getAnnotation(implAnnotation))).contains(api);
	}

	static PublishesBy declaredSet(Set<Class<?>> apis) {
		return (api, impl) -> apis.contains(api);
	}

	/**
	 * Is used as {@link Lift} to add a {@link
	 * #declaredSet(Set)} if "global" API types to the {@link PublishesBy}
	 * strategy set.
	 * <p>
	 * This uses the {@link Lift} so that during the
	 * bootstrapping of the {@link Env} the API {@link Class}es can be added
	 * from different software modules. When the {@link PublishesBy} configured
	 * is resolved it gets decorated by the set.
	 * <p>
	 * Note that a default {@link PublishesBy} must be set explicitly and that
	 * it must be bound with scoping so that {@link Lift}
	 * occurs.
	 *
	 * {@inheritDoc}
	 */
	static PublishesBy liftDeclaredSet(PublishesBy target, Type<?> as,
			Injector context) {
		Class<?>[] apis = context.resolve(Plugins.class) //
				.targeting(Env.class) //
				.forPoint(PublishesBy.class);
		return apis.length == 0
				? target
				: target.or(declaredSet(new HashSet<>(asList(apis))));
	}
}
