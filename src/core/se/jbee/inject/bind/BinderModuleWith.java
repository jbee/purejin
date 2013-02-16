/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.bootstrap.Bootstrapper;
import se.jbee.inject.bootstrap.Bundle;
import se.jbee.inject.bootstrap.Inspector;
import se.jbee.inject.bootstrap.PresetModule;
import se.jbee.inject.config.Presets;

/**
 * The default utility {@link PresetModule}.
 * 
 * A {@link BinderModuleWith} is also a {@link Bundle} so it should be used and installed as such.
 * It will than {@link Bundle#bootstrap(Bootstrapper)} itself as a module.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class BinderModuleWith<T>
		extends AbstractBinderModule
		implements Bundle, PresetModule<T> {

	@Override
	public final void bootstrap( Bootstrapper bootstrap ) {
		bootstrap.install( this );
	}

	@Override
	public final void declare( Bindings bindings, Inspector inspector, T preset ) {
		init( bindings, inspector );
		declare( preset );
	}

	@Override
	public String toString() {
		Type<?> preset = Type.supertype( PresetModule.class, Type.raw( getClass() ) ).parameter( 0 );
		return "module " + getClass().getSimpleName() + "[" + preset + "]";
	}

	/**
	 * @see PresetModule#declare(Bindings, Inspector, Object)
	 * @param preset
	 *            The value contained in the {@link Presets} for the type of this
	 *            {@link PresetModule}.
	 */
	protected abstract void declare( T preset );
}
