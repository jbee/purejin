/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.action;

import se.jbee.lang.Type;

import static se.jbee.lang.Type.raw;

/**
 * The low user level representation of an action (a operation or
 * micro-service).
 *
 * @see ActionExecutor
 *
 * @param <A> The type of the input
 * @param <B> The type of the output
 */
@FunctionalInterface
public interface Action<A, B> {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <A, B> Type<Action<A, B>> actionTypeOf(Type<A> in, Type<B> out) {
		return (Type) raw(Action.class).parameterized(in, out);
	}

	static <A, B> Type<Action<A, B>> actionTypeOf(Class<A> in, Class<B> out) {
		return actionTypeOf(raw(in), raw(out));
	}

	/**
	 * Runs the action to compute the output from the input. This typically does
	 * not change any inner state but it might in rare cases be part of an
	 * action to do such a thing.
	 *
	 * @param input might be null for {@link Void} arguments or when argument
	 *            was resolved to null
	 * @return might be null
	 * @throws ActionExecutionFailed wraps all {@link Exception}s thrown by the
	 *             underlying method.
	 */
	B run(A input) throws ActionExecutionFailed;

}
