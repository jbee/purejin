package de.jbee.inject.service;

/**
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <T>
 *            The return type of the method wired to this service
 */
public interface Service<P, T> {

	T invoke( P params );
}
