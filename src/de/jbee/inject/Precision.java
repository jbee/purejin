package de.jbee.inject;

import java.util.Comparator;

public class Precision {

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
}
