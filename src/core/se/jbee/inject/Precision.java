/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package de.jbee.inject;

import java.util.Comparator;

public final class Precision {

	public static final Comparator<Resourcing<?>> RESOURCE_COMPARATOR = new ResourcingComparator();

	public static <T extends PreciserThan<? super T>> Comparator<T> comparator() {
		return new PreciserThanComparator<T>();
	}

	public static <T extends PreciserThan<? super T>> int comparePrecision( T one, T other ) {
		if ( one.morePreciseThan( other ) ) {
			return -1;
		}
		if ( other.morePreciseThan( one ) ) {
			return 1;
		}
		return 0;
	}

	public static <T extends PreciserThan<? super T>, T2 extends PreciserThan<? super T2>> boolean morePreciseThan2(
			T one, T other, T2 sndOne, T2 sndOther ) {
		return one.morePreciseThan( other ) || !other.morePreciseThan( one )
				&& sndOne.morePreciseThan( sndOther );
	}

	private static class PreciserThanComparator<T extends PreciserThan<? super T>>
			implements Comparator<T> {

		PreciserThanComparator() {
			// make visible
		}

		@Override
		public int compare( T one, T other ) {
			return Precision.comparePrecision( one, other );
		}

	}

	private static final class ResourcingComparator
			implements Comparator<Resourcing<?>> {

		ResourcingComparator() {
			// make visible
		}

		@Override
		public int compare( Resourcing<?> one, Resourcing<?> other ) {
			Resource<?> rOne = one.getResource();
			Resource<?> rOther = other.getResource();
			Class<?> rawOne = rOne.getType().getRawType();
			Class<?> rawOther = rOther.getType().getRawType();
			if ( rawOne != rawOther ) {
				return rawOne.getCanonicalName().compareTo( rawOther.getCanonicalName() );
			}
			return comparePrecision( rOne, rOther );
		}
	}
}
