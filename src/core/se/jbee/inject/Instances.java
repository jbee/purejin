/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.util.Arrays;

/**
 * A hierarchy of {@link Instance}s.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Instances
		implements MorePreciseThan<Instances> {

	public static final Instances ANY = new Instances( new Instance<?>[0] );

	private final Instance<?>[] hierarchy;

	private Instances( Instance<?>... hierarchy ) {
		super();
		this.hierarchy = hierarchy;
	}

	public boolean isAny() {
		return hierarchy.length == 0;
	}

	public int depth() {
		return hierarchy.length;
	}

	public Instance<?> at( int depth ) {
		return hierarchy[depth];
	}

	public Instances push( Instance<?> top ) {
		if ( isAny() ) {
			return new Instances( top );
		}
		return new Instances( Array.prepand( top, hierarchy ) );
	}

	public boolean equalTo( Instances other ) {
		return this == other || Arrays.equals( other.hierarchy, hierarchy );
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof Instances && equalTo( (Instances) obj );
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode( hierarchy );
	}

	@Override
	public String toString() {
		return isAny()
			? "*"
			: Arrays.toString( hierarchy );
	}

	/**
	 * @return true when this has higher depth or equal depth and the first more precise hierarchy
	 *         element.
	 */
	@Override
	public boolean morePreciseThan( Instances other ) {
		if ( this == other ) {
			return false;
		}
		if ( hierarchy.length != other.hierarchy.length ) {
			return hierarchy.length > other.hierarchy.length;
		}
		for ( int i = 0; i < hierarchy.length; i++ ) {
			if ( hierarchy[i].morePreciseThan( other.hierarchy[i] ) ) {
				return true;
			}
			if ( other.hierarchy[i].morePreciseThan( hierarchy[i] ) ) {
				return false;
			}
		}
		return false;
	}
}
