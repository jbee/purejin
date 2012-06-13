package de.jbee.inject.bind;


public interface Module {

	void configure( Bindings bindings );

	//void configure( Context context ); //OPEN just allow this in root-module
}
