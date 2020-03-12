/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.bootstrap.Bootstrapper;
import se.jbee.inject.config.Env;
import se.jbee.inject.config.Options;
import se.jbee.inject.declare.Bundle;
import se.jbee.inject.declare.ModuleWith;

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
		implements Bundle, ModuleWith<T> { //TODO can this extends BinderModule?

	@Override
	public final void bootstrap(Bootstrapper bootstrap) {
		bootstrap.install(BuildinBundle.SUB_CONTEXT);
		bootstrap.install(DefaultScopes.class);
		bootstrap.install(SPIModule.class);
		bootstrap.install(AnnotatedWithModule.class);
		bootstrap.install(this);
	}

	@Override
	public void declare(Bindings bindings, Env env, T option) {
		__init__(configure(env), bindings);
		declare(option);
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
	 * @see ModuleWith#declare(Bindings, Object)
	 * @param preset The value contained in the {@link Options} for the type of
	 *            this {@link ModuleWith}.
	 */
	protected abstract void declare(T preset);
}
