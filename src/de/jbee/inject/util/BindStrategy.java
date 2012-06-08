package de.jbee.inject.util;

import java.lang.reflect.Constructor;

/**
 * OPEN maybe its good to make this part of the core and a part of the BindDeclarator. Thereby first
 * the Injector gets a strategy as a argument and it passes it further down the during the binding
 * itself
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 */
public interface BindStrategy {

	<T> Constructor<T> defaultConstructor( Class<T> type );
}
