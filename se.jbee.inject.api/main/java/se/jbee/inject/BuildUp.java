/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import se.jbee.inject.lang.Type;

import java.lang.annotation.Annotation;
import java.util.function.Predicate;

import static se.jbee.inject.lang.Type.classType;
import static se.jbee.inject.lang.Type.raw;

/**
 * A {@link BuildUp} is similar to a "post construct interceptor" that is
 * called after an object is created. In contrast to a classic post construct
 * the {@link BuildUp} concept allow to modify, wrap or even replace the
 * instance that is build-up.
 * <p>
 * {@link BuildUp}s are matched based on the actual type of the target argument.
 * If target can be assigned to the {@link BuildUp}'s target type the {@link
 * BuildUp#buildUp(Object, Type, Injector)} is called.
 * <p>
 * {@link BuildUp}s allow to run initialisation code once and build more
 * powerful mechanisms on top of it.
 * <p>
 * A {@link BuildUp} can also implement {@link Predicate} of {@link Class} to
 * add a {@link Class} based filter so it does not automatically apply to all
 * instances that are assignable to its type parameter.
 * <p>
 * For example an {@link Annotation} based match can be done by declaring a
 * {@link BuildUp} for {@link Object} with a {@link Predicate} checking that
 * passed {@link Class} for the annotation in question.
 *
 * @param <T> type of the value build, all values assignable to this type
 *            will match
 * @since 8.1
 */
@FunctionalInterface
public interface BuildUp<T> { // BuildUp + TearDown

	static <T> Type<BuildUp<T>> buildUpTypeOf(Class<T> initialisedType) {
		return buildUpTypeOf(raw(initialisedType));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <T> Type<BuildUp<T>> buildUpTypeOf(Type<T> initialisedType) {
		return (Type) raw(BuildUp.class).parametized(initialisedType);
	}

	/**
	 * Is called when the target instance is created within the {@link Injector}
	 * context. For {@link Injector} itself as target this is called as soon as
	 * its state has been initialised. It can be used to decorate the {@link
	 * Injector} or any other target instance created within a context.
	 *
	 * @param target  the newly created instance to build-up. For {@link
	 *                BuildUp} of the {@link Injector} this is always the
	 *                decorated {@link Injector} in case decoration was done by
	 *                other {@link BuildUp}s.
	 * @param as      the type as which the target instance is used (injected).
	 *                While this is the actual instance type or a supertype of
	 *                it this is not necessarily a supertype of the {@link
	 *                BuildUp}s supertype of the actual type of the target
	 *                so it cannot use the type parameter {@code T}.
	 * @param context use to resolve instances that require further
	 *                initialisation setup. For {@link BuildUp} of the
	 *                {@link Injector} this is always the underlying {@link
	 *                Injector} that isn't decorated in case decoration was done
	 *                before by another {@link BuildUp}.
	 * @return the initialised instance; usually this is still the target
	 * instance. When using proxy or decorator pattern this would be the proxy
	 * or decorator instance.
	 */
	T buildUp(T target, Type<?> as, Injector context);

	/**
	 * Mostly defined to capture the contract by convention that when a {@link
	 * BuildUp} class does implement {@link Predicate} of {@link Class} the
	 * {@link Predicate#test(Object)} method is used to check if the {@link
	 * BuildUp} should be applied to the actual build-up target type. This
	 * allows {@link BuildUp} to only apply to a subset of instances even though
	 * their type signature matches the target instance.
	 * <p>
	 * While such a check could be done within the {@link BuildUp}s
	 * implementation implementing the {@link Predicate} prevents non matching
	 * {@link BuildUp}s from being invoked at all.
	 *
	 * @return true, if this {@link BuildUp} does support the {@link
	 * #asFilter()} method and wants it to be used.
	 */
	default boolean isFiltered() {
		return this instanceof Predicate
				&& classType(getClass()).isAssignableTo( //
						raw(Predicate.class).parametized(Class.class));
	}

	/**
	 * @return This {@link BuildUp} as {@link Predicate} to test if a actual
	 * instance target class should be build-up by this {@link BuildUp}.
	 *
	 * @see #isFiltered()
	 */
	@SuppressWarnings("unchecked")
	default Predicate<Class<?>> asFilter() {
		return (Predicate<Class<?>>) this;
	}

	/**
	 * Can be bound to implement a defined custom ordering in which
	 * {@link BuildUp}s are applied by an {@link Injector} context.
	 *
	 * @since 8.1
	 */
	@FunctionalInterface
	interface Sequencer {

		/**
		 * Called for each actual type once to sort the {@link BuildUp}s
		 * that apply for that type.
		 *
		 * @param actualType the actual type of the build-up object
		 * @param set the set if {@link BuildUp}s to order, this is not
		 *            (necessarily) the full set of {@link BuildUp} defined
		 *            in a {@link Injector} context but a set as it applies to
		 *            the initialisation of a particular instance.
		 * @return the sorted set, the set might also be sorted in place. Return
		 *         is mostly for convenience using this within expressions like
		 *         lambdas.
		 */
		BuildUp<?>[] order(Class<?> actualType, BuildUp<?>[] set);
	}

}
