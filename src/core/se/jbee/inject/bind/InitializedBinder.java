/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import static se.jbee.inject.bootstrap.Bindings.bindings;
import se.jbee.inject.Source;
import se.jbee.inject.bind.Binder.RootBinder;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Inspect;
import se.jbee.inject.bootstrap.Macros;
import se.jbee.inject.container.Scope;
import se.jbee.inject.container.Scoped;

/**
 * A {@link RootBinder} that can be initialized using the {@link #init(Bindings)} method.
 * 
 * This allows to change the start {@link Bind} once.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class InitializedBinder
		extends RootBinder {

	private Bind bind;
	private Boolean initialized;

	protected InitializedBinder() {
		this( Scoped.APPLICATION, null );
	}

	protected InitializedBinder( Scope inital, Source source ) {
		super( Bind.create( bindings( Macros.DEFAULT, Inspect.DEFAULT ), source, inital ) );
		this.bind = super.bind();
	}

	@Override
	final Bind bind() {
		return bind;
	}

	protected final void init( Bindings bindings ) {
		Bootstrap.nonnullThrowsReentranceException( initialized );
		this.bind = super.bind().into( bindings );
		initialized = true;
	}

}