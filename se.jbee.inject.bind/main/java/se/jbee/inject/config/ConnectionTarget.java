package se.jbee.inject.config;

import se.jbee.lang.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static se.jbee.lang.Type.actualReturnType;

public final class ConnectionTarget {

	public final Object instance;
	public final Type<?> as;
	public final Method connected;
	public final Invoke invoke;

	public ConnectionTarget(Object instance, Type<?> as, Method connected,
			Invoke invoke) {
		this.instance = instance;
		this.as = as;
		this.connected = connected;
		this.invoke = invoke;
	}

	public Type<?> returnType() {
		return actualReturnType(connected, as);
	}

	public <A, B> boolean isUsableFor(Type<A> in, Type<B> out) {
		if (!returnType().equalTo(out))
			return false;
		if (in.equalTo(Type.VOID) && connected.getParameterCount() == 0)
			return true;
		for (Parameter p : connected.getParameters()) {
			if (Type.actualParameterType(p, as).equalTo(in))
				return true;
		}
		return false;
	}
}
