package se.jbee.inject.config;

import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.lang.Type;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static java.util.Arrays.asList;

@FunctionalInterface
public interface ContractsBy {

	/**
	 * @param api  the raw class type of a super-class or super-interface of the
	 *             provided implementation type
	 * @param impl the implementation type that fulfills the api
	 * @return True, if the given api type should be considered a contract
	 * (for the given implementation type), else false
	 */
	boolean isContract(Class<?> api, Class<?> impl);

	/**
	 * No API is accepted. Useful to build upon when e.g. using only a {@link
	 * #declaredSet(Set)}.
	 */
	ContractsBy NONE = (api, impl) -> false;

	/**
	 * All APIs are accepted which is the same as binding any and all of the
	 * super-types of a type.
	 */
	ContractsBy SUPER = (api, impl) -> true;

	/**
	 * Binds only APIs that are interfaces unless the implementation does not
	 * implement any directly (it might still inherit some).
	 */
	ContractsBy PROTECTIVE = (api, impl) -> api.isInterface() || api == impl && impl.getInterfaces().length == 0;

	/**
	 * Binds all APIs that are interface types <b>and</b> the exact
	 * implementation class itself.
	 */
	ContractsBy OPTIMISTIC = (api, impl) -> api.isInterface() || api == impl;

	/**
	 * Note that this optimizes and can also return this or the other {@link
	 * ContractsBy} strategy in case an OR would result in same logic anyhow
	 * since {@link #NONE} or {@link #SUPER} is involved.
	 *
	 * @param other another, alternative strategy
	 * @return a {@link ContractsBy} strategy that considers an API a contract
	 * if it either matches this condition or the provided other
	 */
	default ContractsBy or(ContractsBy other) {
		if (this == SUPER || other == NONE) return this;
		if (this == NONE) return other;
		return (api, impl) -> isContract(api, impl) || other.isContract(api, impl);
	}

	default ContractsBy and(ContractsBy other) {
		if (other == SUPER || this == NONE) return this;
		if (this == SUPER || other == NONE) return other;
		return (api, impl) -> isContract(api, impl) && other.isContract(api, impl);
	}

	default ContractsBy annotatedWith(Class<? extends Annotation> apiAnnotation) {
		return (api, impl) -> api.isAnnotationPresent(apiAnnotation) //
				&& isContract(api, impl);
	}

	default <T extends Annotation> ContractsBy annotatedWith(
			Class<T> implAnnotation, Function<T, Class<?>[]> contracts) {
		return (api, impl) -> impl.isAnnotationPresent(implAnnotation) && asList(
				contracts.apply(impl.getAnnotation(implAnnotation))).contains(api);
	}

	static ContractsBy declaredSet(Set<Class<?>> apis) {
		return (api, impl) -> apis.contains(api);
	}

	/**
	 * Is used as {@link se.jbee.inject.BuildUp} to add a {@link
	 * #declaredSet(Set)} if "global" API types to the {@link ContractsBy}
	 * strategy set.
	 * <p>
	 * This uses the {@link se.jbee.inject.BuildUp} so that during the
	 * bootstrapping of the {@link Env} the API {@link Class}es can be added
	 * from different software modules. When the {@link ContractsBy} configured
	 * is resolved it gets decorated by the set.
	 * <p>
	 * Note that a default {@link ContractsBy} must be set explicitly and that
	 * it must be bound with scoping so that {@link se.jbee.inject.BuildUp}
	 * occurs.
	 *
	 * {@inheritDoc}
	 */
	static ContractsBy buildUpDeclaredSet(ContractsBy target, Type<?> as,
			Injector context) {
		Class<?>[] apis = context.resolve(Plugins.class) //
				.targeting(Env.class) //
				.forPoint(ContractsBy.class);
		return apis.length == 0
				? target
				: target.or(declaredSet(new HashSet<>(asList(apis))));
	}
}
