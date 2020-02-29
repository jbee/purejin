package se.jbee.inject;

/**
 * Marker interface to mark classes that should be constructed "ad-hoc" by a
 * {@link Injector} context.
 * 
 * This means the marked type is (most likely) not explicitly bound itself but
 * it depends on bound instances that should be injected into its
 * {@link Utils#commonConstructor(Class)}.
 * 
 * This is used to extend existing SPIs by wrapping or combining them.
 * 
 * @since 19.1
 */
public interface SPI {
	// marker
}
