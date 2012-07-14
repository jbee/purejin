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

	/**
	 * OPEN this is in some way against the concept of silk because the information which instances
	 * belong where came from the code so this should be something one could build on top of silk
	 * but not a core part ---> e.g. HintStrategy interface build in builder - user could create an
	 * constant that looks for hints
	 */
	<T> Instance<?>[] parametersFor( Constructor<T> constructor );

	//<T> InjectionPoint<?>[] injectionPointsFor(Class<T> type);

}
