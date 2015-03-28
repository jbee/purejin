/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.Source;
import se.jbee.inject.bootstrap.Binding;
import se.jbee.inject.bootstrap.Macro;
import se.jbee.inject.container.Scoped;

/**
 * A {@link MacroModule} is used to make the {@link Binder} API available in {@link Macro}s itself.
 * This way the a {@link Macro} does not need to be defined on the low level of {@link Binding}s.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class MacroModule
		extends BinderModule {

	protected MacroModule( Source source ) {
		super( Scoped.APPLICATION, source, null );
	}

}