package de.jbee.silk;

/**
 * Knows how to resolve a specific instance for the given dependency.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <T>
 */
public interface ResourceResolver<T> {

	T resolve( Dependency<T> dependency );
}
