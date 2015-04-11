package se.jbee.inject;

/**
 * If there is a statically resolvable problem with a binding (resource in
 * the context of a container) this exception is thrown during
 * bootstrapping. It is never thrown after the bootstrapping step has
 * finished (a {@link Injector} was created successfully).
 */
public final class BindingIsInconsistent extends RuntimeException {

	public BindingIsInconsistent(String message) {
		super(message);
	}
	
}