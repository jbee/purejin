package de.jbee.inject.bind;

import de.jbee.inject.InjectionStrategy;

public interface Module {

	void declare( Bindings bindings, InjectionStrategy strategy );

}
