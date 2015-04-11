/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * A kind of singleton for a {@link Resource} inside a {@link Injector}.
 * 
 * Another way to look at it is to see it as a production rule or factory for a
 * particular instance or a family of instances.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public interface Injectron<T> {

	/**
	 * @return The instance created or resolved for the given {@link Dependency}.
	 */
	T instanceFor( Dependency<? super T> dependency );
	
	InjectronInfo<T> info();
}
