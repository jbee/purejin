package de.jbee.silk;

/**
 * Describes what is wanted/needed as parameter to construct a instance of T.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <T>
 */
public interface Dependency<T>
		extends Reference<T> {

	int resourceNr();

	int resourceCardinality();
}
