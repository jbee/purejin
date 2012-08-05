package de.jbee.inject;

/**
 * Hints are *not* about to find/identify the constructor to use! This would get far to complex to
 * understand (yet simple to implement) using different constructors depending on the {@link Hint}s.
 * 
 * {@linkplain Hint}s identify the {@link Instance} to use for the constructor specified or given by
 * {@link InjectionStrategy}!
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Hint {
	// just a marker for now
}
