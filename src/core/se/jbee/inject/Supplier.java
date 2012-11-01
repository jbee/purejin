/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * A {@link Supplier} is a source or factory for specific instances.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <T>
 *            The type of the instance being resolved
 */
public interface Supplier<T> {

	/**
	 * This {@link Supplier} is asked to supply the instance that should be used the given
	 * {@link Dependency}.
	 * 
	 * @param injector
	 *            The {@link Injector} is used to resolve {@link Dependency}s during a possible
	 *            object creation of the returned instance.
	 * @return the instance created or resolved.
	 */
	T supply( Dependency<? super T> dependency, Injector injector );

}
