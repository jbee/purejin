/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.container;

import java.lang.annotation.Annotation;
import java.util.function.Predicate;

import se.jbee.inject.Injector;

/**
 * An {@link Initialiser} is like an interceptor that is called after an object
 * is created.
 * 
 * {@link Initialiser}s are matched based on the actual type of the target
 * argument. If target can be assigned to the {@link Initialiser}'s target type
 * the {@link Initialiser#init(Object, Injector)} is called.
 * 
 * {@link Initialiser}s allow to run initialisation code once and build more
 * powerful mechanisms on top of it.
 * 
 * An {@link Initialiser} can also implement {@link Predicate} of {@link Class}
 * to add a {@link Class} based filter so it does not automatically apply to all
 * instances that are assignable to its type parameter.
 * 
 * For example an {@link Annotation} based match can be done by declaring a
 * {@link Initialiser} for {@link Object} with a {@link Predicate} checking that
 * passed {@link Class} for the annotation in question.
 * 
 * @param <T> type of the value initialised, all values assignable to this type
 *            will match
 * 
 * @since 19.1
 */
@FunctionalInterface
public interface Initialiser<T> {

	/**
	 * Is called when the target instance is created within the {@link Injector}
	 * context. For {@link Injector} itself as target this is called as soon as
	 * its state has been initialised. It can be used to decorate the
	 * {@link Injector} or any other target instance created within a context.
	 * 
	 * @param target the newly created instance to initialise. For
	 *            {@link Initialiser} of the {@link Injector} this is always the
	 *            decorated {@link Injector} in case decoration was done by
	 *            other {@link Initialiser}s.
	 * @param context use to resolve instances that require further
	 *            initialisation setup. For {@link Initialiser} of the
	 *            {@link Injector} this is always the underlying
	 *            {@link Injector} that isn't decorated in case decoration was
	 *            done before by another {@link Initialiser}.
	 * @return the initialised instance; usually this is still the target
	 *         instance. When using proxy or decorator pattern this would be the
	 *         proxy or decorator instance.
	 */
	T init(T target, Injector context);

	/**
	 * Can be bound to implement a defined custom ordering in which
	 * {@link Initialiser}s are applied by an {@link Injector} context.
	 * 
	 * @since 19.1
	 */
	@FunctionalInterface
	interface Sorter {

		/**
		 * Called for each actual type once to sort the {@link Initialiser}s
		 * that apply for that type.
		 * 
		 * @param actualType the actual type of the initialised object
		 * @param set the set if {@link Initialiser}s to order, this is not
		 *            (necessarily) the full set of {@link Initialiser} defined
		 *            in a {@link Injector} context but a set as it applies to
		 *            the initialisation of a particular instance.
		 * @return the sorted set, the set might also be sorted in place. Return
		 *         is mostly for convenience using this within expressions like
		 *         lambdas.
		 */
		Initialiser<?>[] sort(Class<?> actualType, Initialiser<?>[] set);
	}

}
