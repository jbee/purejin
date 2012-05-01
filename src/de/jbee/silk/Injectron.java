package de.jbee.silk;

/**
 * A kind of singleton for a {@link Resource} inside a {@link Injector}.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Injectron<T> {

	Resource<T> getResource();

	Source getSource();

	//OPEN maybe we change this so that it s a real 'inject' method that gets a receiver passed as one argument ? The receiver is a interface that accepts the instance: The InjectionPoint
	T provide( Dependency<T> dependency, DependencyContext context );

}
