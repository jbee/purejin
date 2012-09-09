package de.jbee.inject.bind;


public interface Module {

	void declare( Bindings bindings, ConstructionStrategy strategy );

}
