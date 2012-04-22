package de.jbee.silk;

/**
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <T>
 *            The type of the instance being resolved
 */
public interface Supplier<T> {

	T supply( Dependency<T> dependency, DependencyResolver resolver );

	// TODO some way to validate the source - is the constructor available etc.
}
