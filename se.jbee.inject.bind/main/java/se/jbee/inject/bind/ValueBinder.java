/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.*;
import se.jbee.inject.binder.Constructs;
import se.jbee.lang.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static se.jbee.lang.Type.classType;
import static se.jbee.lang.Type.raw;

/**
 * A {@linkplain ValueBinder} is a pure function that transforms a source value
 * {@link Descriptor} of a certain type to one or more complete {@link Binding}s
 * that are added to the target set of {@link Bindings}.
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
 * Known {@link Descriptor}s are:
 * <ul>
 * <li>{@link Constructs}: Creates instances from
 * {@link Constructor}</li>
 * <li>{@link se.jbee.inject.binder.Produces}: Creates instances from a
 * factory {@link Method}</li>
 * <li>{@link se.jbee.inject.binder.Constant}: Provides an instance from a
 * constant value.</li>
 * <li>{@link se.jbee.inject.Descriptor.BridgeDescriptor}: Creates a reference to the target type</li>
 * <li>{@link Instance}: Creates a reference to the {@link Instance}</li>
 * <li>{@link se.jbee.inject.Descriptor.ArrayDescriptor}: Creates an array instance with elements lazily
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
 * @param <D> The type of source value that is expanded by this {@link
 *            ValueBinder}
 */
@FunctionalInterface
public interface ValueBinder<D extends Descriptor> {

	@SuppressWarnings({"unchecked", "rawtypes"})
	static <T extends Descriptor> Type<ValueBinder<? extends T>> valueBinderTypeOf(Class<T> type) {
		return (Type) raw(ValueBinder.class).parameterized(classType(type));
	}

	/**
	 * Expands the incomplete {@link Binding} and {@link Descriptor} value given
	 * to one or more complete {@link Binding}(s) that are added to {@link
	 * Bindings}.
	 *
	 * @param ref  A {@link Descriptor} value. Based upon the type of the {@link
	 *             Descriptor} the {@link ValueBinder} to use was selected. It
	 *             holds the information the {@link ValueBinder} needs to
	 *             further process the {@link Binding}.
	 * @param item A usually incomplete {@link Binding} (without a {@link
	 *             Supplier}) encoding all the information about the binding in
	 *             progress.
	 * @param dest complete {@link Binding}s are be added to it
	 */
	<T> void expand(Env env, D ref, Binding<T> item, Bindings dest);

	/**
	 * A {@link Completion} just uses the passed {@link Descriptor} value to
	 * {@link Completion#complete(Env, Binding, Descriptor)} the {@link Binding}
	 * and add it to the {@link Bindings}. Its main purpose to to capture this
	 * recurring pattern, reduce the duplication caused by it and make the code
	 * more readable.
	 */
	@FunctionalInterface
	interface Completion<D extends Descriptor> extends ValueBinder<D> {

		@Override
		default <T> void expand(Env env, D ref, Binding<T> item,
				Bindings dest) {
			dest.addExpanded(env, complete(env, item, ref));
		}

		<T> Binding<T> complete(Env env, Binding<T> item, D ref);
	}
}
