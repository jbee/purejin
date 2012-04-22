package de.jbee.silk;

/**
 * Knows how to build things (might be scoped too).
 * 
 * Knows how to create instances by Identity. A identity is a logical reference to a object. That
 * object already exists or will be created.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface DependencyResolver {

	<T> T resolve( Dependency<T> dependency );
}
