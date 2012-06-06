package de.jbee.inject;

public interface Module {

	Module BUILD_IN = new BuildinModule();

	void configure( BindDeclarator binder );

	//void configure( Context context ); //OPEN just allow this in root-module
}
