/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.declare;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import se.jbee.inject.Env;
import se.jbee.inject.Instance;
import se.jbee.inject.Locator;
import se.jbee.inject.Parameter;
import se.jbee.inject.container.Supplier;

/**
 * A {@linkplain ValueBinder} is a pure function that transforms a source value
 * of a certain type to one or more complete {@link Binding}s that are added to
 * the target set of {@link Bindings}.
 * 
 * <h3>How {@link ValueBinder}s Work</h3> Instead of binding a a {@link Locator}
 * to a specific {@link Supplier} in one go a source value is created that holds
 * all information required to create an appropriate {@link Binding} with a
 * concrete {@link Supplier} from it. This allows to use the same binder API
 * without specifying exactly what {@link Supplier}s are used to e.g.
 * instantiate instances from a {@link Constructor}. Instead there is a set of
 * known data types (source values). For each of them a matching
 * {@link ValueBinder} is bound in the {@link Env}. When the value is expanded
 * the matching {@link ValueBinder} is resolved and used to convert the source
 * value into complete {@link Binding}s. This allows to customise and add logic
 * on low level.
 * 
 * The core source values are:
 * <ul>
 * <li>{@link se.jbee.inject.bootstrap.New}: Creates instances from
 * {@link Constructor}</li>
 * <li>{@link se.jbee.inject.bootstrap.Produces}: Creates instances from a
 * factory {@link Method}</li>
 * <li>{@link se.jbee.inject.bootstrap.Constant}: Provides an instance from a
 * constant value.</li>
 * <li>{@link Class}: Creates a reference to the target type</li>
 * <li>{@link Instance}: Creates a reference to the {@link Instance}</li>
 * <li>{@link Parameter[]}: Creates an array instance with elements lazily
 * resolved from the {@link Parameter}s</li>
 * <li>{@link Binding}: It is its task to actually do
 * {@link Bindings#add(Binding)}. All other {@link ValueBinder}s should use
 * {@link Bindings#addExpanded(Binding)} (called with a {@link Binding} as
 * value) so that any {@link Binding} created can be inspected by the
 * {@link ValueBinder} for {@link Binding} which might derive further
 * {@link Binding}s.</li>
 * </ul>
 * 
 * Further custom types can be added. In that case the {@link Env} must be
 * extended to include the {@link ValueBinder} that can handle the custom type.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <V> The type of value that is expanded by this {@link ValueBinder}
 */
@FunctionalInterface
public interface ValueBinder<V> {

	/**
	 * Expands the incomplete {@link Binding} and value given to a complete
	 * {@link Binding}(s) that are added to {@link Bindings}.
	 * 
	 * @param src A {@link Class}, {@link Instance} or similar values that
	 *            express the intent of the incomplete binding. This
	 *            {@link ValueBinder} will use them especially to decide the
	 *            {@link Supplier} used.
	 * @param item A usually incomplete {@link Binding} (without a
	 *            {@link Supplier})
	 * @param target The collection of {@link Bindings} complete
	 *            {@link Binding}s should be added to.
	 */
	<T> void expand(Env env, V src, Binding<T> item, Bindings target);

	/**
	 * A {@link Completion} just uses the passed value to
	 * {@link Completion#complete(Binding, Object)} the {@link Binding} and add
	 * it to the {@link Bindings}.
	 */
	interface Completion<V> extends ValueBinder<V> {

		@Override
		public default <T> void expand(Env env, V src, Binding<T> item,
				Bindings target) {
			target.addExpanded(env, complete(item, src));
		}

		<T> Binding<T> complete(Binding<T> item, V src);
	}
}
