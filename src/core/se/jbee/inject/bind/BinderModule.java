/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.Scope;

/**
 * The default utility {@link Module} almost always used.
 * 
 * A {@link BinderModule} is also a {@link Bundle} so it should be used and installed as such. It
 * will than {@link Bundle#bootstrap(Bootstrapper)} itself as a module.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class BinderModule
		extends AbstractBinderModule
		implements Bundle, Module {

	protected BinderModule() {
		super();
	}

	protected BinderModule( Scope inital ) {
		super( inital );
	}

	@Override
	public final void bootstrap( Bootstrapper bootstrap ) {
		bootstrap.install( this );
	}

	@Override
	public final void declare( Bindings bindings, Inspector inspector ) {
		init( bindings, inspector );
		declare();
	}

	/**
	 * @see Module#declare(Bindings, Inspector)
	 */
	protected abstract void declare();

}
