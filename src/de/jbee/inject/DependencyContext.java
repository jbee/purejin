package de.jbee.inject;

/**
 * Knows how to *resolve* the instance for a given {@link Dependency}.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface DependencyContext {

	<T> T resolve( Dependency<T> dependency );
}
