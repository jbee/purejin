package de.jbee.silk;

/**
 * Describes what is wanted/needed as parameter to construct a instance of T.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <T>
 */
public interface Dependency<T> {

	DefiniteType<T> getType();

	int resourceNr();

	int resourceCardinality();

	// also add target hierarchy: the class of the instance that is injected
}
