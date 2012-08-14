package de.jbee.inject.bind;

import de.jbee.inject.Scope;

public interface BasicConfigurator {

	ScopedBasicConfigurator let( Scope scope );

	interface ScopedBasicConfigurator {

		void outlive( Scope... scopes );

		void equate( Scope... scopes );

		void snapshot( Scope... scopes );
	}

}
