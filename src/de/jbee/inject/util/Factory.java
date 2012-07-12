package de.jbee.inject.util;

import de.jbee.inject.Instance;

public interface Factory<T> {

	<P> T produce( Instance<? super T> produced, Instance<P> injected );
}
