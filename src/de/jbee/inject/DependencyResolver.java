package de.jbee.inject;

/**
 * Knows how to resolve a specific instance for the given dependency.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <T>
 */
public interface DependencyResolver<T> {

	T resolve( Dependency<T> dependency );
}
