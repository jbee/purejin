package de.jbee.inject.util;

/**
 * Not a core concept. OPEN move ?
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Provider<T> {

	T provide();
}
