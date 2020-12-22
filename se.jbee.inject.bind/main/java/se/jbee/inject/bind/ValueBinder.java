/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.*;
import se.jbee.inject.binder.Constructs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * A {@linkplain ValueBinder} is a pure function that transforms a source value
 * {@link Ref} of a certain type to one or more complete {@link Binding}s that
 * are added to the target set of {@link Bindings}.
 *
 * <h2>How {@link ValueBinder}s Work</h2>
 * Instead of binding a a {@link Locator} to a specific {@link Supplier} in one
 * go a source value is created that holds all information required to create an
 * appropriate {@link Binding} with a concrete {@link Supplier} from it. This
 * allows to use the same binder API without specifying exactly what {@link
 * Supplier}s are used to e.g. instantiate instances from a {@link Constructor}.
 * Instead there is a set of known data types (source values). For each of them
 * a matching {@link ValueBinder} is bound in the {@link Env}. When the value is
 * expanded the matching {@link ValueBinder} is resolved and used to convert the
 * source value into complete {@link Binding}s. This allows to customise and add
 * logic on low level.
 * <p>
 * The core source values are:
 * <ul>
 * <li>{@link Constructs}: Creates instances from
 * {@link Constructor}</li>
 * <li>{@link se.jbee.inject.binder.Produces}: Creates instances from a
 * factory {@link Method}</li>
 * <li>{@link se.jbee.inject.binder.Constant}: Provides an instance from a
 * constant value.</li>
 * <li>{@link Class}: Creates a reference to the target type</li>
 * <li>{@link Instance}: Creates a reference to the {@link Instance}</li>
 * <li>{@link se.jbee.inject.Hint[]}: Creates an array instance with elements lazily
 * resolved from the {@link se.jbee.inject.Hint}s</li>
 * <li>{@link Binding}: It is its task to actually do
 * {@link Bindings#add(Env, Binding)}. All other {@link ValueBinder}s should use
 * {@link Bindings#addExpanded(Env, Binding)} (called with a {@link Binding} as
 * value) so that any {@link Binding} created can be inspected by the
 * {@link ValueBinder} for {@link Binding} which might derive further
 * {@link Binding}s.</li>
 * </ul>
 * <p>
 * Further custom types can be added. In that case the {@link Env} must be
 * extended to include the {@link ValueBinder} that can handle the custom type.
 *
 * @param <R> The type of source value that is expanded by this {@link
 *            ValueBinder}
 */
@FunctionalInterface
public interface ValueBinder<R extends Ref> {

	/**
	 * Expands the incomplete {@link Binding} and value given to a complete
	 * {@link Binding}(s) that are added to {@link Bindings}.
	 *
	 * @param ref    A {@link Class}, {@link Instance} or similar value that
	 *               express the intent of the incomplete binding. This {@link
	 *               ValueBinder} will use it especially to decide the {@link
	 *               Supplier} used and what further {@link Binding}s might be
	 *               derived.
	 * @param item   A usually incomplete {@link Binding} (without a {@link
	 *               Supplier})
	 * @param target complete {@link Binding}s are be added to it
	 */
	<T> void expand(Env env, R ref, Binding<T> item, Bindings target);

	/**
	 * A {@link Completion} just uses the passed value to {@link
	 * Completion#complete(Env, Binding, Ref)} the {@link Binding} and add it to
	 * the {@link Bindings}. Its main purpose to to capture this recurring
	 * pattern, reduce the duplication caused by it and make the code more
	 * readable.
	 */
	@FunctionalInterface
	interface Completion<R extends Ref> extends ValueBinder<R> {

		@Override
		default <T> void expand(Env env, R ref, Binding<T> item,
				Bindings target) {
			target.addExpanded(env, complete(env, item, ref));
		}

		<T> Binding<T> complete(Env env, Binding<T> item, R ref);
	}
}
