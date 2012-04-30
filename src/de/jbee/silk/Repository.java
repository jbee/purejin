package de.jbee.silk;

/**
 * Manages the already created instances.
 * 
 * Existing instances are returned, non-existing are received from the given
 * {@link DependencyResolver} and stocked forever.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 */
public interface Repository {

	<T> T yield( Dependency<T> dependency, DependencyResolver<T> resolver );
}
