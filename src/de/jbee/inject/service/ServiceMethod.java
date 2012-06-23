package de.jbee.inject.service;

/**
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <R>
 *            The return type of the method wired to this service
 */
public interface ServiceMethod<P, R> {

	R invoke( P params );

}
