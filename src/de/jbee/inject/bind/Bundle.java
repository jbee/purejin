package de.jbee.inject.bind;


/**
 * A bundle installs sub-bundles and {@link Module}s.
 * 
 * All {@link Bundle}s are real singletons. A bundle means you get X without a when or but. X all
 * the time.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Bundle {

	void bootstrap( Bootstrapper bootstrap );
}
