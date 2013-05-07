/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import static se.jbee.inject.Source.source;
import static se.jbee.inject.bootstrap.Bindings.bindings;
import se.jbee.inject.Scope;
import se.jbee.inject.bind.Binder.RootBinder;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Inspect;
import se.jbee.inject.bootstrap.Inspector;
import se.jbee.inject.util.Scoped;

/**
 * A {@link RootBinder} that can be initialized using the {@link #init(Bindings, Inspector)} method.
 * 
 * This allows to change the start {@link Bind} once.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class InitializedBinder
		extends RootBinder {

	private Boolean initialized;
	private Bind bind;

	protected InitializedBinder() {
		this( Scoped.APPLICATION );
	}

	protected InitializedBinder( Scope inital ) {
		super( Bind.create( bindings( SuppliedBy.MACROS, Inspect.DEFAULT ),
				source( InitializedBinder.class ), inital ) );
		this.bind = super.bind();
	}

	@Override
	final Bind bind() {
		return bind;
	}

	protected final void init( Bindings bindings ) {
		Bootstrap.nonnullThrowsReentranceException( initialized );
		this.bind = super.bind().into( bindings ).using( bindings.getInspector() ).with(
				source( getClass() ) );
		initialized = true;
	}

}