package de.jbee.inject;

/**
 * Gives read-only access to the binds done.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 */
public interface Binding<T> {

	Resource<T> getResource();

	Supplier<T> getSupplier();

	Source getSource();

	Repository getRepository();
}
