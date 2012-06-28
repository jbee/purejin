package de.jbee.inject;

/**
 * A kind of singleton for a {@link Resource} inside a {@link Injector}.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Injectron<T> {

	Resource<T> getResource();

	Source getSource();

	T instanceFor( Dependency<? super T> dependency );

}
