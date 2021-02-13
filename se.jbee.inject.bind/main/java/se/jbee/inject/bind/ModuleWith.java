/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.Env;
import se.jbee.inject.Name;
import se.jbee.lang.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static se.jbee.lang.Type.raw;

/**
 * A {@link ModuleWith} is an extension to a usual {@link Module} that depends
 * on *one* of the values that have been set in the {@link Env}. The property is
 * resolved by type.
 *
 * {@link ModuleWith} are also used to apply the effects of custom
 * {@link Annotation}s. In that case the property passed is the annotated
 * {@link Class}.
 *
 * @see Module
 *
 * @param <T> The type of the property value
 */
@FunctionalInterface
public interface ModuleWith<T> extends Module {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	Type<ModuleWith<Class<?>>> TYPE_ANNOTATION = //
			(Type) raw(ModuleWith.class).parameterized(Type.CLASS);

	@SuppressWarnings({ "unchecked", "rawtypes" })
	Type<ModuleWith<Method>> METHOD_ANNOTATION = //
			(Type) raw(ModuleWith.class).parameterized(Method.class);

	/**
	 * @param bindings use to declare made bound within this {@link Module}.
	 * @param property The set value for the type (maybe null in case no value
	 *            was set or the value was set to null)
	 */
	void declare(Bindings bindings, Env env, T property);

	@Override
	default void declare(Bindings bindings, Env env) {
		Type<?> valueType = raw(getClass()).toSuperType(ModuleWith.class).parameter(0);
		@SuppressWarnings("unchecked")
		final T value = valueType.rawType == Env.class
			? (T) env
			: (T) env.property(Name.DEFAULT, valueType,
					getClass());
		declare(bindings, env, value);
	}
}
