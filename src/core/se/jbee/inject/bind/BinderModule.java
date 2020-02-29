/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.bootstrap.Bootstrapper;
import se.jbee.inject.bootstrap.Bundle;
import se.jbee.inject.bootstrap.Module;

/**
 * The default utility {@link Module} almost always used.
 *
 * A {@link BinderModule} is also a {@link Bundle} so it should be used and
 * installed as such. It will than {@link Bundle#bootstrap(Bootstrapper)} itself
 * as a module.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class BinderModule extends InitializedBinder
		implements Bundle, Module {

	private final Class<? extends Bundle> basis;

	protected BinderModule() {
		this(null);
	}

	protected BinderModule(Class<? extends Bundle> basis) {
		this.basis = basis;
	}

	@Override
	public final void bootstrap(Bootstrapper bootstrap) {
		if (basis != null)
			bootstrap.install(basis);
		bootstrap.install(DefaultScopes.class);
		bootstrap.install(SPIModule.class);
		bootstrap.install(AnnotatedWithModule.class);
		bootstrap.install(this);
	}

	@Override
	public final void declare(Bindings bindings) {
		__init__(configure(bindings));
		declare();
	}

	protected Bindings configure(Bindings bindings) {
		return bindings;
	}

	@Override
	public String toString() {
		return "module " + getClass().getSimpleName();
	}

	/**
	 * @see Module#declare(Bindings)
	 */
	protected abstract void declare();
}
