package se.jbee.inject;

/**
 * Marker interface to mark classes should extend the {@link Injector} API.
 * 
 * This means the marked type does not need to be bound explicitly. A singleton
 * instance per type is created using the type's
 * {@link Utils#commonConstructor(Class)}.
 * 
 * @since 19.1
 */
public interface SPI {
	// marker
}
