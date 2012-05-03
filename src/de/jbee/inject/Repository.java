package de.jbee.inject;

/**
 * Manages the already created instances.
 * 
 * Existing instances are returned, non-existing are received from the given
 * {@link Injectable} and stocked forever.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 */
public interface Repository {

	<T> T yield( Injection<T> injection, Injectable<T> injectable );
}
