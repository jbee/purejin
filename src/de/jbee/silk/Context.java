package de.jbee.silk;

/**
 * OPEN if we constrain {@link Context} declarations to the root-{@link Module} and have a separate
 * method for that we can be sure we know about the relation between the scopes and such before any
 * bind will be made.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Context {

	ScopeDeclarator consider( Scope scope );

	/**
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 */
	interface ScopeDeclarator {

		void within( Scope scope );
	}

}
