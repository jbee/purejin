package de.jbee.inject;

/**
 * A {@link Supplier} is asked to supply the instance that should be used for a particular
 * {@link Dependency}.
 * 
 * The {@link Injector} should be used to resolve dependencies during object creation.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <T>
 *            The type of the instance being resolved
 */
public interface Supplier<T> {

	T supply( Dependency<? super T> dependency, Injector injector );

}
