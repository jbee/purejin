package de.jbee.silk.builder;

import de.jbee.silk.Scope;


/**
 * The ROOT- {@link RootBinder}.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface RootBinder
		extends PresetBinder.ScopedBinder {

	ScopedBinder in( Scope scope );

}
