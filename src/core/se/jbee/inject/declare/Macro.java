/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.declare;

import java.lang.reflect.Constructor;

import se.jbee.inject.Instance;
import se.jbee.inject.Locator;
import se.jbee.inject.bootstrap.Binding;
import se.jbee.inject.bootstrap.BindingType;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.config.Env;
import se.jbee.inject.container.Supplier;

/**
 * A {@linkplain Macro} is a prepared set of instructions that transform the
 * passed value into a one or more complete {@link Binding}s that are added to
 * the set of {@link Bindings}.
 * 
 * <h3>How Macros Work</h3> The macro mechanism might seem confusing at first
 * glance. It is used to "decide" what {@link Supplier} to use when a
 * {@link Locator} is bound to some object through the binder API. The macro
 * expansion is complete when the incomplete {@link Binding} is
 * {@link Binding#complete(BindingType, Supplier)} with some specific
 * {@link Supplier}.
 * 
 * A macro registers for a specific {@link Class} of objects. For example to
 * instances of type {@link Constructor}. When such a instance is encountered
 * during macro expansion both {@link Binding} and the instance (value) are
 * passed to the macro for further processing. The macro either completes it or
 * transforms the value for further expansion. It may also derive values from
 * the instances and expand those as well, like the parameters of a constructor.
 * 
 * This makes the binder API independent of specific {@link Supplier}s. So even
 * if one uses <code>bind(...).toConstructor()</code> the actual
 * {@link Supplier} used is a result of macro expansion what allows to customise
 * behaviour but sill use the binder API as is.
 * 
 * The most important {@link Macro} is the one bound for {@link Binding} itself.
 * It is its task to actually do {@link Bindings#add(Binding)}. All other
 * {@link Macro}s should use {@link Bindings#addExpanded(Binding)} (called with
 * a {@link Binding} value) so that any meta-expansion can be taken care of in
 * the {@link Macro} bound for {@link Binding}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <V> The type of value that is expanded by this macro
 */
@FunctionalInterface
public interface Macro<V> { //TODO renamed to Connector?

	/**
	 * Expands the incomplete {@link Binding} and value given to a complete
	 * {@link Binding}(s) that are added to {@link Bindings}.
	 * 
	 * @param value A {@link Class}, {@link Instance} or similar values that
	 *            express the intent of the incomplete binding. This
	 *            {@link Macro} will use them especially to decide the
	 *            {@link Supplier} used.
	 * @param incomplete A usually incomplete {@link Binding} (without a
	 *            {@link Supplier})
	 * @param bindings The collection of {@link Bindings} complete
	 *            {@link Binding}s should be added to.
	 */
	<T> void expand(Env env, V value, Binding<T> incomplete,
			Bindings bindings);

	/**
	 * A {@link Completion} just uses the passed value to
	 * {@link Completion#complete(Binding, Object)} the {@link Binding} and add
	 * it to the {@link Bindings}.
	 */
	interface Completion<V> extends Macro<V> {

		@Override
		public default <T> void expand(Env env, V value,
				Binding<T> incomplete, Bindings bindings) {
			bindings.addExpanded(env, complete(incomplete, value));
		}

		<T> Binding<T> complete(Binding<T> incomplete, V value);
	}
}
