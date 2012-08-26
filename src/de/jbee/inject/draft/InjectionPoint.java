package de.jbee.inject.draft;

import de.jbee.inject.Instance;

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
