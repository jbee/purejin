package de.jbee.inject;

/**
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @deprecated Not used
 */
@Deprecated
public interface InjectionPoint<T> {

	void inject( T instance );
}
