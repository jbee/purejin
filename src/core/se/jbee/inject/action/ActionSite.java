package se.jbee.inject.action;

import static se.jbee.inject.Utils.accessible;

import java.lang.reflect.Method;

import se.jbee.inject.Type;

public final class ActionSite<I, O> {

	public final Object impl;
	public final Method action;
	public final Type<I> input;
	public final Type<O> output;

	public ActionSite(Object impl, Method action, Type<I> input,
			Type<O> output) {
		this.impl = impl;
		this.action = accessible(action);
		this.input = input;
		this.output = output;
	}

	@Override
	public String toString() {
		return input + " => " + output + " [" + action.toGenericString() + "]";
	}
}
