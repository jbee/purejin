package de.jbee.inject;

/**
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <T>
 *            The type of the instance being resolved
 */
public interface Supplier<T> {

	T supply( Dependency<? super T> dependency, DependencyResolver context );

	// TODO some way to validate the source - is the constructor available etc.
}
