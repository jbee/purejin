package de.jbee.inject;

import static de.jbee.inject.Precision.morePreciseThan2;
import static de.jbee.inject.Type.raw;

/**
 * Describes WHAT can be injected and WHERE it can be injected.
 * 
 * It is an {@link Instance} with added information where the bind applies.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public final class Resource<T>
		implements Typed<T>, Named, PreciserThan<Resource<?>> {

	public static <T> Resource<T> resource( Class<T> type ) {
		return new Resource<T>( Instance.anyOf( raw( type ) ) );
	}

	private final Instance<T> instance;
	private final Target target;

	public Resource( Instance<T> instance ) {
		this( instance, Target.ANY );
	}

	public Resource( Instance<T> instance, Target target ) {
		super();
		this.instance = instance;
		this.target = target;
	}

	public boolean isApplicableFor( Dependency<? super T> dependency ) {
		return isAvailableFor( dependency ) // 
				&& isAdequateFor( dependency ) //
				&& isAssignableTo( dependency ); // most 'expensive' check so we do it last
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
		return target.isApplicableFor( dependency );
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

	@Override
	public boolean morePreciseThan( Resource<?> other ) {
		return morePreciseThan2( instance, other.instance, target, other.target );
	}

	@Override
	public String toString() {
		return target + "-" + instance;
	}

	@Override
	public Name getName() {
		return instance.getName();
	}

	public Instance<T> getInstance() {
		return instance;
	}

	@Override
	public <E> Resource<E> typed( Type<E> type ) {
		return new Resource<E>( instance.typed( type ), target );
	}

	public boolean includes( Resource<?> other ) {
		//TODO add target
		return instance.includes( other.instance );
	}
}
