package de.jbee.silk;

/**
 * A kind of singleton for a {@link Resource} inside a {@link Injector}.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Injectron<T> {

	Resource<T> getResource();

	T yield( Dependency<T> dependency, DependencyContext context );

}
