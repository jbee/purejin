package de.jbee.inject;

import de.jbee.inject.util.SuppliableInjector;

/**
 * A kind of singleton for a {@link Resource} inside a {@link SuppliableInjector}.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Injectron<T> {

	Resource<T> getResource();

	Source getSource();

	T instanceFor( Dependency<? super T> dependency );

	Expiry getExpiry();

}
