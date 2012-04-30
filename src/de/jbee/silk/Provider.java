package de.jbee.silk;

/**
 * Not a core concept. OPEN move to base ?
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Provider<T> {

	T yield();
}
