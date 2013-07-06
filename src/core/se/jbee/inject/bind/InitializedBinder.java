/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import static se.jbee.inject.bootstrap.Bindings.bindings;
import se.jbee.inject.Scope;
import se.jbee.inject.Source;
import se.jbee.inject.bind.Binder.RootBinder;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Inspect;
import se.jbee.inject.bootstrap.Macros;
import se.jbee.inject.util.Scoped;

/**
 * A {@link RootBinder} that can be initialized using the {@link #init(Bindings)} method.
 * 
 * This allows to change the start {@link Bind} once.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class InitializedBinder
		extends RootBinder {

	private final Source source;
	private Bind bind;
	private Boolean initialized;

	protected InitializedBinder() {
		this( Scoped.APPLICATION );
	}

	protected InitializedBinder( Scope inital ) {
		this( inital, null );
	}

	protected InitializedBinder( Source source ) {
		this( Scoped.APPLICATION, source );
	}

	private InitializedBinder( Scope inital, Source source ) {
		super( Bind.create( bindings( Macros.DEFAULT, Inspect.DEFAULT ), source, inital ) );
		this.bind = super.bind();
		this.source = source;
	}

	@Override
	final Bind bind() {
		return bind;
	}

	protected final void init( Bindings bindings ) {
		Bootstrap.nonnullThrowsReentranceException( initialized );
		this.bind = super.bind().into( bindings ).with( source() );
		initialized = true;
	}

	private Source source() {
		return this.source == null
			? Source.source( getClass() )
			: this.source;
	}

}