package de.jbee.inject.util;

import de.jbee.inject.Scope;


/**
 * The ROOT- {@link RootBinder}.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface RootBinder
		extends PresetBinder.ScopedBinder {

	ScopedBinder in( Scope scope );

}
