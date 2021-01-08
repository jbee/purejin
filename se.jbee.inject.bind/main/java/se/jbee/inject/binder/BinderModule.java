/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.binder;

import se.jbee.inject.Env;
import se.jbee.inject.Name;
import se.jbee.inject.Scope;
import se.jbee.inject.ScopeLifeCycle;
import se.jbee.inject.bind.Bindings;
import se.jbee.inject.bind.Bootstrapper;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.Module;

import static se.jbee.inject.Scope.container;

/**
 * The default utility {@link Module} almost always used.
 *
 * A {@link BinderModule} is also a {@link Bundle} so it should be used and
 * installed as such. It will than {@link Bundle#bootstrap(Bootstrapper)} itself
 * as a module.
 */
public abstract class BinderModule extends AbstractBinderModule
		implements Module {

	private final Class<? extends Bundle> basis;

	protected BinderModule() {
		this(null);
	}

	protected BinderModule(Class<? extends Bundle> basis) {
		this.basis = basis;
	}

	/**
	 * @see Module#declare(Bindings, Env)
	 */
	protected abstract void declare();

	@Override
	public final void bootstrap(Bootstrapper bootstrap) {
		if (installDefaults())
			bootstrap.installDefaults();
		if (basis != null)
			bootstrap.install(basis);
		bootstrap.install(this);
		installAnnotated(getClass(), bootstrap);
	}

	@Override
	public final void declare(Bindings bindings, Env env) {
		__init__(configure(env), bindings);
		declare();
	}

	/**
	 * Override this to customise the {@link Env} used within this {@link
	 * BinderModule}.
	 *
	 * @param env The "global" {@link Env}
	 * @return the adjusted {@link Env} for this {@link BinderModule}
	 */
	public Env configure(Env env) {
		return env;
	}

	protected final <P> P env(Class<P> property) {
		return env().property(property);
	}

	@Override
	public String toString() {
		return "module " + getClass().getSimpleName();
	}

	/**
	 * Binds a {@link ScopeLifeCycle} with the needed {@link Scope#container}.
	 *
	 * @since 8.1
	 * @param lifeCycle the instance to bind, not null
	 */
	protected final void bindLifeCycle(ScopeLifeCycle lifeCycle) {
		bindLifeCycle(lifeCycle.scope).to(lifeCycle);
	}

	protected final TypedBinder<ScopeLifeCycle> bindLifeCycle(Name scope) {
		return per(container).bind(scope, ScopeLifeCycle.class);
	}

	/**
	 * Starts the binding of a {@link Scope}.
	 *
	 * @since 8.1
	 * @param scope name of the scope to create
	 * @return fluent API to invoke one of the {@code to} methods to provide the
	 *         {@link Scope} or the indirection creating it.
	 */
	protected final TypedBinder<Scope> bindScope(Name scope) {
		return per(container).bind(scope, Scope.class);
	}

	protected boolean installDefaults() {
		return true;
	}
}
