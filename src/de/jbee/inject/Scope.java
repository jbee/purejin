package de.jbee.inject;

public interface Scope {

	/**
	 * Creates a empty {@link Repository} that stores instances in this {@link Scope}.
	 * 
	 * @return a empty instance in this {@linkplain Scope}.
	 */
	Repository init();
}
