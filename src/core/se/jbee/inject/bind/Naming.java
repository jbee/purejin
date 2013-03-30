package se.jbee.inject.bind;

import se.jbee.inject.Name;

public interface Naming<T> {

	Name name( T value );
}
