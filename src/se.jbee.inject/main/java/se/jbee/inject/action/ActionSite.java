package se.jbee.inject.action;

import static se.jbee.inject.Utils.accessible;

import java.lang.reflect.Method;

import se.jbee.inject.Type;

/**
 * Describes a unique action implementation point. That is the particular
 * {@link Method} that implements the {@link Action} for the input parameter
 * {@link Type}.
 * 
 * @author Jan Bernitt
 *
 * @param <I> Type of the input parameter
 * @param <O> Type of the output value
 * 
 * @since 19.1
 */
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