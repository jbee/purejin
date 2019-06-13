package se.jbee.inject.action;

import static se.jbee.inject.Utils.accessible;

import java.lang.reflect.Method;

import se.jbee.inject.Type;

public final class ActionSite<I, O> {

	/**
	 * The instance implementing the {@link #action} {@link Method}.
	 */
	public final Object owner;
	public final Method action;
	public final Type<I> input;
	public final Type<O> output;

	public ActionSite(Object owner, Method action, Type<I> input,
			Type<O> output) {
		this.owner = owner;
		this.action = accessible(action);
		this.input = input;
		this.output = output;
	}

	@Override
	public String toString() {
		return input + " => " + output + " [" + action.toGenericString() + "]";
	}
}
