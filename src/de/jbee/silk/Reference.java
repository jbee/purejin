package de.jbee.silk;

import java.lang.reflect.ParameterizedType;

/**
 * What do we want exactly ? Kind of a key
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <T>
 */
public interface Reference<T> {

	ParameterizedType getParameterizedType();

	// use this as key to instances/sources

	// needs access to the receiver type (stack) ?

	boolean fulfills( Dependency<T> dependency );

	boolean morePreciseThan( Reference<T> other );
}
