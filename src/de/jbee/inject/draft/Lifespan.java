package de.jbee.inject.draft;

import de.jbee.inject.Dependency;

public interface Lifespan<T> {

	T origination( Dependency<?> dependency );

	Lifecycle cycle( T origination );

}
