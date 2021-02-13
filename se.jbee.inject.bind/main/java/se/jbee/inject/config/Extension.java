package se.jbee.inject.config;

import se.jbee.inject.Injector;
import se.jbee.lang.Utils;

/**
 * Marker interface to mark classes representing an {@link Injector} API
 * {@link Extension}.
 *
 * This means the marked type does not need to be bound explicitly. A singleton
 * instance per type is created using the type's
 * {@link Utils#mostVisibleMostParametersConstructor(Class)}.
 *
 * @since 8.1
 */
public interface Extension {
	// marker
}
