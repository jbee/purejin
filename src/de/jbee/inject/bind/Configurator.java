package de.jbee.inject.bind;

import de.jbee.inject.Scope;

public interface Configurator {

	ScopedConfigurator consider( Scope scope );

	/**
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 */
	interface ScopedConfigurator {

		void within( Scope scope );
	}

}
