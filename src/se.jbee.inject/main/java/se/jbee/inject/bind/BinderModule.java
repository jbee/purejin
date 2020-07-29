/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import static se.jbee.inject.Scope.container;

import se.jbee.inject.Env;
import se.jbee.inject.Name;
import se.jbee.inject.Scope;
import se.jbee.inject.ScopePermanence;
import se.jbee.inject.declare.Bindings;
import se.jbee.inject.declare.Bootstrapper;
import se.jbee.inject.declare.Bundle;
import se.jbee.inject.declare.Module;

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
		bootstrap.install(DefaultsBundle.class);
		if (basis != null)
			bootstrap.install(basis);
		bootstrap.install(this);
	}

	@Override
	public void declare(Bindings bindings, Env env) {
		__init__(configure(env), bindings);
		declare();
	}

	protected Env configure(Env env) {
		return env;
	}

	protected final <P> P env(Class<P> property) {
		return env().property(property, bind().source.pkg());
	}

	@Override
	public String toString() {
		return "module " + getClass().getSimpleName();
	}

	/**
	 * Binds a {@link ScopePermanence} with the needed {@link Scope#container}.
	 * 
	 * @since 19.1
	 * @param sp instance to bind, not null
	 */
	protected final void bindScopePermanence(ScopePermanence sp) {
		per(container).bind(sp.scope, ScopePermanence.class).to(sp);
	}

	/**
	 * Starts the binding of a {@link Scope}.
	 * 
	 * @since 19.1
	 * @param scope name of the scope to create
	 * @return fluent API to invoke one of the {@code to} methods to provide the
	 *         {@link Scope} or the indirection creating it.
	 */
	protected final TypedBinder<Scope> bindScope(Name scope) {
		return per(container).bind(scope, Scope.class);
	}

	/**
	 * @see Module#declare(Bindings, Env)
	 */
	protected abstract void declare();
}
