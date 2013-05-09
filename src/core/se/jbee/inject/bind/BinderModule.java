/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.Scope;
import se.jbee.inject.Source;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.bootstrap.Bootstrapper;
import se.jbee.inject.bootstrap.Bundle;
import se.jbee.inject.bootstrap.Inspector;
import se.jbee.inject.bootstrap.Module;

/**
 * The default utility {@link Module} almost always used.
 * 
 * A {@link BinderModule} is also a {@link Bundle} so it should be used and installed as such. It
 * will than {@link Bundle#bootstrap(Bootstrapper)} itself as a module.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class BinderModule
		extends InitializedBinder
		implements Bundle, Module {

	protected BinderModule() {
		super();
	}

	protected BinderModule( Scope inital ) {
		super( inital );
	}

	protected BinderModule( Source source ) {
		super( source );
	}

	@Override
	public final void bootstrap( Bootstrapper bootstrap ) {
		bootstrap.install( this );
	}

	@Override
	public final void declare( Bindings bindings ) {
		init( bindings );
		declare();
	}

	@Override
	public String toString() {
		return "module " + getClass().getSimpleName();
	}

	/**
	 * @see Module#declare(Bindings, Inspector)
	 */
	protected abstract void declare();

}
