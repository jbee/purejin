package de.jbee.silk;


/**
 * The ROOT- {@link RootBinder}.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface RootBinder
		extends PresetBinder.ScopedBinder {

	ScopedBinder in( Scope scope );

}
