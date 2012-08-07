package de.jbee.inject;

/**
 * Knows how to resolve a specific instance for the given {@link Demand}.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <T>
 */
public interface Injectable<T> {

	T instanceFor( Demand<T> demand );
}
