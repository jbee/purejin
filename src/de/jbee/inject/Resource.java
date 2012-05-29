package de.jbee.inject;

/**
 * Describes WHAT can be injected and WHERE it can be injected.
 * 
 * It is an {@link Instance} with added information where the bind applies.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public final class Resource<T>
		implements Typed<T>, Comparable<Resource<?>> {

	private final Instance<T> instance;
	private final Availability availability;

	public Resource( Instance<T> instance ) {
		this( instance, Availability.EVERYWHERE );
	}

	public Resource( Instance<T> instance, Availability availability ) {
		super();
		this.instance = instance;
		this.availability = availability;
	}

	public boolean isApplicableFor( Dependency<? super T> dependency ) {
		return isAvailableFor( dependency ) // 
				&& isAdequateFor( dependency ) //
				&& isAssignableTo( dependency );
	}

	/**
	 * Does the {@link Type} of this a valid argument for the one of the {@link Dependency} given ?
	 */
	public boolean isAssignableTo( Dependency<? super T> dependency ) {
		return instance.getType().isAssignableTo( dependency.getType() );
	}

	/**
	 * Does the given {@link Dependency} occur in the right package and for the right target ?
	 */
	public boolean isAvailableFor( Dependency<? super T> dependency ) {
		return availability.isApplicableFor( dependency );
	}

	/**
	 * Does this resource provide the instance wanted by the given {@link Dependency}'s {@link Name}
	 */
	public boolean isAdequateFor( Dependency<? super T> dependency ) {
		return instance.getName().isApplicableFor( dependency.getName() );
	}

	@Override
	public Type<T> getType() {
		return instance.getType();
	}

	public boolean morePreciseThan( Resource<T> other ) {
		return compareTo( other ) == 1;
	}

	@Override
	public int compareTo( Resource<?> other ) {
		// check type

		// check location (package)

		// check discriminator
		return 0;
	}

	@Override
	public String toString() {
		return availability + "-" + instance;
	}
}
