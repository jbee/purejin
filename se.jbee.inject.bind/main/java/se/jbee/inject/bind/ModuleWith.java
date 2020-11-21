/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import static se.jbee.inject.lang.Type.raw;

import java.lang.annotation.Annotation;

import se.jbee.inject.Env;
import se.jbee.inject.Name;
import se.jbee.inject.lang.Type;

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
 * @author Jan Bernitt (jan@jbee.se)
 *
 * @param <T> The type of the property value
 */
@FunctionalInterface
public interface ModuleWith<T> extends Module {

	@SuppressWarnings({ "unchecked", "rawtypes" }) Type<ModuleWith<Class<?>>> ANNOTATION = (Type) raw(
			ModuleWith.class).parametized(Type.CLASS);

	/**
	 * @param bindings use to declare made bound within this {@link Module}.
	 * @param property The set value for the type (maybe null in case no value
	 *            was set or the value was set to null)
	 */
	void declare(Bindings bindings, Env env, T property);

	@Override
	default void declare(Bindings bindings, Env env) {
		Type<?> valueType = Type.supertype(ModuleWith.class,
				raw(getClass())).parameter(0);
		@SuppressWarnings("unchecked")
		final T value = valueType.rawType == Env.class
			? (T) env
			: (T) env.property(Name.DEFAULT, valueType,
					getClass().getPackage());
		declare(bindings, env, value);
	}

}
