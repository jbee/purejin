package de.jbee.silk;

/**
 * A special {@link Provider} that represents the binding used to provide the new instance (if
 * needed).
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <T>
 */
public interface ResourceResolver<T> {

	T resolve( Dependency<T> dependency );
}
