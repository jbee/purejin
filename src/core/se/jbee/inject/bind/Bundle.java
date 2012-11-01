/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

/**
 * A bundle installs sub-bundles and {@link Module}s.
 * 
 * All {@link Bundle}s are real singletons. A bundle means you get X without a when or but. X all
 * the time.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Bundle {

	/**
	 * @param bootstrap
	 *            The {@link Bootstrapper} this {@link Bundle} should install itself to.
	 */
	void bootstrap( Bootstrapper bootstrap );
}
