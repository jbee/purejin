package de.jbee.inject.bind;

import de.jbee.inject.ConstructionStrategy;

public interface Module {

	void declare( Bindings bindings, ConstructionStrategy strategy );

}
