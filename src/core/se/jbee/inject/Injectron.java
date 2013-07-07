/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * A kind of singleton for a {@link Resource} inside a {@link Injector}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public interface Injectron<T>
		extends Resourced<T> {

	/**
	 * @return The {@link Source} that {@link Injection} had been created from (e.g. did define the
	 *         bind).
	 */
	Source getSource();

	/**
	 * @return The instance created or resolved for the given {@link Dependency}.
	 */
	T instanceFor( Dependency<? super T> dependency );

	/**
	 * @return The frequency in which this injectron's {@link Resource} expires.
	 */
	Expiry getExpiry();

}
