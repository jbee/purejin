package de.jbee.inject;

/**
 * This about the HOW to inject (field, constructor/setter)
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @deprecated Not used
 */
@Deprecated
public interface InjectionPoint<T> {

	void inject( T instance );

	Instance<T> getInstance();
}
