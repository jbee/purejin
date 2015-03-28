/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static se.jbee.inject.Type.raw;

/**
 * Describes WHAT can be injected and WHERE it can be injected.
 * 
 * It is an {@link Instance} with added information where the bind applies.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Resource<T>
		implements Typed<T>, MorePreciseThan<Resource<?>> {

	public static <T> Resource<T> resource( Class<T> type ) {
		return new Resource<T>( Instance.anyOf( raw( type ) ) );
	}

	public final Instance<T> instance;
	public final Target target;

	public Resource( Instance<T> instance ) {
		this( instance, Target.ANY );
	}

	public Resource( Instance<T> instance, Target target ) {
		super();
		this.instance = instance;
		this.target = target;
	}

	public boolean isApplicableFor( Dependency<? super T> dependency ) {
		return isAdequateFor( dependency ) // check names first since default goes sorts first but will not match any named 
				&& isAvailableFor( dependency )//
				&& isAssignableTo( dependency ); // most 'expensive' check so we do it last
	}

	public boolean isSuitableFor( Dependency<? super T> dependency ) {
		return isAdequateFor( dependency ) && isAssignableTo( dependency );
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
		return instance.name.isCompatibleWith( dependency.getName() );
	}

	@Override
	public Type<T> getType() {
		return instance.getType();
	}

	@Override
	public boolean morePreciseThan( Resource<?> other ) {
		return Instance.morePreciseThan2( instance, other.instance, target, other.target );
	}

	@Override
	public String toString() {
		return instance + " " + target;
	}

	public Name getName() {
		return instance.name;
	}

	@Override
	public <E> Resource<E> typed( Type<E> type ) {
		return new Resource<E>( instance.typed( type ), target );
	}

	public boolean equalTo( Resource<?> other ) {
		return this == other 
				|| instance.equalTo( other.instance ) && target.equalTo( other.target );
	}
}
