package de.jbee.inject.util;

import java.lang.reflect.Constructor;

public interface BindStrategy {

	<T> Constructor<T> defaultConstructor( Class<T> type );
}
