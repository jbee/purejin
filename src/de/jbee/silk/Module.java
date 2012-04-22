package de.jbee.silk;

public interface Module {

	void configure( Binder binder );

	void configure( Context context ); //OPEN just allow this in root-module
}
