/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.Env;
import se.jbee.inject.bind.Binder.RootBinder;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.declare.Bindings;

/**
 * A {@link RootBinder} that can be initialized using the
 * {@link #__init__(Bindings)} method.
 *
 * This allows to change the start {@link Bind} once.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class InitializedBinder extends RootBinder {

	private Bind bind;
	private Boolean initialized;

	protected InitializedBinder() {
		super(Bind.UNINITIALIZED);
		this.bind = super.bind();
	}

	@Override
	final Bind bind() {
		return bind;
	}

	protected final void __init__(Env env, Bindings bindings) {
		Bootstrap.nonnullThrowsReentranceException(initialized);
		this.bind = init(bind.into(env, bindings));
		initialized = true;
	}

	protected Bind init(Bind bind) {
		return bind;
	}

}