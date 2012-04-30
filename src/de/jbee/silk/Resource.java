package de.jbee.silk;

import java.lang.reflect.Type;

/**
 * Describes WHAT can be injected and WHERE it can be injected.
 * 
 * It is an {@link Instance} with added information where the bind applies.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public final class Resource<T> { // OPEN use Injectron for Resource and find another name here ?

	// Object has the meaning of ANY: e.g. binding Object to a concrete instance mean use that whenever possible (and no other more precise binding applies)

	public static <T> Dependency<T> type( Type type ) {
		return null;
	}

	public boolean fulfills( Dependency<T> dependency ) {
		return false;
	}

	public DefiniteType<T> getType() {

		return null;
	}

	public boolean morePreciseThan( Resource<T> other ) {
		// check type

		// check location (package)

		// check discriminator
		return false;
	}
}
