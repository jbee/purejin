package de.jbee.silk;

public interface Lifespan<T> {

	T origination( Dependency<?> dependency );

	Lifecycle cycle( T origination );

}
