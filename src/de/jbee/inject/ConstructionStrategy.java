package de.jbee.inject;

import java.lang.reflect.Constructor;

/**
 * A {@link ConstructionStrategy} picks the {@link Constructor} to use to construct objects of a
 * given {@link Class}.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface ConstructionStrategy {

	/**
	 * @return The {@link Constructor} considered to be the reasonable or right way to construct a
	 *         object of the given type. In case one with parameters is returned the process will
	 *         try to resolve them.
	 */
	<T> Constructor<T> constructorFor( Class<T> type );

}
