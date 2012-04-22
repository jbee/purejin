package de.jbee.silk;

public interface Module<T extends RootBinder> {

	void configure( T binder );

	void configure( Context context ); //OPEN just allow this in root-module
}
