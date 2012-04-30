package de.jbee.silk.scope;

import de.jbee.silk.Dependency;

public interface Lifespan<T> {

	T origination( Dependency<?> dependency );

	Lifecycle cycle( T origination );

}
