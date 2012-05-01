package de.jbee.silk;

/**
 * Describes WHAT can be injected and WHERE it can be injected.
 * 
 * It is an {@link Instance} with added information where the bind applies.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public final class Resource<T>
		implements Comparable<Resource<T>> {

	// Object has the meaning of ANY: e.g. binding Object to a concrete instance mean use that whenever possible (and no other more precise binding applies)

	public static <T> Dependency<T> type( java.lang.reflect.Type type ) {
		return null;
	}

	public boolean isApplicableFor( Dependency<T> dependency ) {
		return isAvailableFor( dependency ) && isAssignableTo( dependency );
	}

	public boolean isAssignableTo( Dependency<T> dependency ) {
		return false;
	}

	boolean isAvailableFor( Dependency<T> dependency ) {

		return false;
	}

	public Type<T> getType() {

		return null;
	}

	public boolean morePreciseThan( Resource<T> other ) {
		// check type

		// check location (package)

		// check discriminator
		return false;
	}

	@Override
	public int compareTo( Resource<T> o ) {
		// TODO Auto-generated method stub
		return 0;
	}
}
