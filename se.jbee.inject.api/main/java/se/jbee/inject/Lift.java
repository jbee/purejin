/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import se.jbee.lang.Type;

import java.lang.annotation.Annotation;
import java.util.function.Predicate;

import static se.jbee.lang.Type.classType;
import static se.jbee.lang.Type.raw;

/**
 * A {@link Lift} is similar to a "post construct interceptor" that is
 * called after an object is created. In contrast to a classic post construct
 * the {@link Lift} concept allow to modify, wrap or even replace the
 * instance that is lifted.
 * <p>
 * {@link Lift}s are matched based on the actual type of the target argument.
 * If a target can be assigned to the {@link Lift}'s target type the {@link
 * Lift#lift(Object, Type, Injector)} is called.
 * <p>
 * {@link Lift}s allow to run initialisation code once and build more
 * powerful mechanisms on top of it.
 * <p>
 * A {@link Lift} can also implement a {@link Predicate} of {@link Class} to
 * add a {@link Class} based filter so it does not automatically apply to all
 * instances that are assignable to its type parameter.
 * <p>
 * For example an {@link Annotation} based match can be done by declaring a
 * {@link Lift} for {@link Object} with a {@link Predicate} checking that
 * passed {@link Class} for the annotation in question.
 *
 * @param <T> type of the value build, all values assignable to this type
 *            will match
 * @since 8.1
 */
@FunctionalInterface
public interface Lift<T> {

	static <T> Type<Lift<T>> liftTypeOf(Class<T> targetType) {
		return liftTypeOf(raw(targetType));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <T> Type<Lift<T>> liftTypeOf(Type<T> targetType) {
		return (Type) raw(Lift.class).parameterized(targetType);
	}

	/**
	 * Is called when the target instance is created within the {@link Injector}
	 * context. For {@link Injector} itself as target this is called as soon as
	 * its state has been initialised. It can be used to decorate the {@link
	 * Injector} or any other target instance created within a context.
	 *
	 * @param target  the newly created instance to build-up. For {@link
	 *                Lift} of the {@link Injector} this is always the
	 *                decorated {@link Injector} in case decoration was done by
	 *                other {@link Lift}s.
	 * @param as      the type as which the target instance is used (injected).
	 *                While this is the actual instance type or a supertype of
	 *                it this is not necessarily a supertype of the {@link
	 *                Lift}s supertype of the actual type of the target
	 *                so it cannot use the type parameter {@code T}.
	 * @param context use to resolve instances that require further
	 *                initialisation setup. For {@link Lift} of the
	 *                {@link Injector} this is always the underlying {@link
	 *                Injector} that isn't decorated in case decoration was done
	 *                before by another {@link Lift}.
	 * @return the initialised instance; usually this is still the target
	 * instance. When using proxy or decorator pattern this would be the proxy
	 * or decorator instance.
	 */
	T lift(T target, Type<?> as, Injector context);

	/**
	 * Using this method is a way to start with a lambda and add the filter this
	 * way in case the {@link Lift} should only apply in a specific filter
	 * condition.
	 *
	 * @param test a {@link Predicate} that given the actual type of the lifted
	 *             instance returns true if this {@link Lift} should be applied,
	 *             else false
	 * @return This {@link Lift} but only called when the test returns true
	 */
	default Lift<T> onlyWhen(Predicate<Class<?>> test) {
		// we need a class here to implement Predicate as well
		class LiftWhen implements Lift<T>, Predicate<Class<?>> {

			@Override
			public boolean test(Class<?> actualType) {
				return test.test(actualType);
			}

			@Override
			public T lift(T target, Type<?> as, Injector context) {
				return Lift.this.lift(target, as, context);
			}
		}
		return new LiftWhen();
	}

	/**
	 * Mostly defined to capture the contract by convention that when a {@link
	 * Lift} class does implement {@link Predicate} of {@link Class} the
	 * {@link Predicate#test(Object)} method is used to check if the {@link
	 * Lift} should be applied to the actual build-up target type. This
	 * allows {@link Lift} to only apply to a subset of instances even though
	 * their type signature matches the target instance.
	 * <p>
	 * While such a check could be done within the {@link Lift}s
	 * implementation implementing the {@link Predicate} prevents non matching
	 * {@link Lift}s from being invoked at all.
	 *
	 * @return true, if this {@link Lift} does support the {@link
	 * #asFilter()} method and wants it to be used.
	 */
	default boolean isFiltered() {
		return this instanceof Predicate
				&& classType(getClass()).isAssignableTo( //
						raw(Predicate.class).parameterized(Class.class));
	}

	/**
	 * @return This {@link Lift} as {@link Predicate} to test if a actual
	 * instance target class should be build-up by this {@link Lift}.
	 *
	 * @see #isFiltered()
	 */
	@SuppressWarnings("unchecked")
	default Predicate<Class<?>> asFilter() {
		return (Predicate<Class<?>>) this;
	}

	/**
	 * Can be bound to implement a defined custom ordering in which
	 * {@link Lift}s are applied by an {@link Injector} context.
	 *
	 * @since 8.1
	 */
	@FunctionalInterface
	interface Sequencer {

		/**
		 * Called for each actual type once to sort the {@link Lift}s
		 * that apply for that type.
		 *
		 * @param actualType the actual type of the build-up object
		 * @param set the set if {@link Lift}s to order, this is not
		 *            (necessarily) the full set of {@link Lift} defined
		 *            in a {@link Injector} context but a set as it applies to
		 *            the initialisation of a particular instance.
		 * @return the sorted set, the set might also be sorted in place. Return
		 *         is mostly for convenience using this within expressions like
		 *         lambdas.
		 */
		Lift<?>[] order(Class<?> actualType, Lift<?>[] set);
	}

}
