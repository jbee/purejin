package de.jbee.inject;

import java.lang.reflect.Constructor;

/**
 * A {@link InjectionStrategy} is kind of a configuration of the dependency injection process.
 * 
 * OPEN Maybe first the Injector gets a strategy as a argument and it passes it further down the
 * during the binding itself
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 */
public interface InjectionStrategy {

	/**
	 * @return The {@link Constructor} considered to be the reasonable or right way to construct a
	 *         object of the given type. In case one with parameters is returned the process will
	 *         try to resolve them.
	 */
	<T> Constructor<T> constructorFor( Class<T> type );

	//<T> InjectionPoint<?>[] injectionPointsFor(Class<T> type);

}
