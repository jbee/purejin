/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.binder;

import java.lang.annotation.Annotation;

import se.jbee.inject.Env;
import se.jbee.inject.Type;
import se.jbee.inject.bind.Bindings;
import se.jbee.inject.bind.Bootstrapper;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.ModuleWith;

/**
 * The default utility {@link ModuleWith}.
 * 
 * A {@link BinderModuleWith} is also a {@link Bundle} so it should be used and
 * installed as such. It will than {@link Bundle#bootstrap(Bootstrapper)} itself
 * as a module.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class BinderModuleWith<T> extends InitializedBinder
		implements Bundle, ModuleWith<T> {

	@Override
	public final void bootstrap(Bootstrapper bootstrap) {
		bootstrap.installDefaults();
		bootstrap.install(this);
	}

	@Override
	public void declare(Bindings bindings, Env env, T property) {
		__init__(configure(env), bindings);
		declare(property);
	}

	protected Env configure(Env env) {
		return env;
	}

	@Override
	public String toString() {
		Type<?> preset = Type.supertype(ModuleWith.class,
				Type.raw(getClass())).parameter(0);
		return "module " + getClass().getSimpleName() + "[" + preset + "]";
	}

	/**
	 * @see ModuleWith#declare(Bindings, Env, Object)
	 * 
	 * @param property The value contained from the {@link Env} by type or the
	 *            annotated {@link Class} in case this {@link ModuleWith}
	 *            defines the effects of a custom {@link Annotation}.
	 */
	protected abstract void declare(T property);
}
