package de.jbee.inject.bind;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import de.jbee.inject.Name;
import de.jbee.inject.Type;

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

	<T> Method factoryFor( Type<T> returnType, Name name, Class<?> implementor );
}
