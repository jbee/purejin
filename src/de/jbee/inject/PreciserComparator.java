package de.jbee.inject;

import java.util.Comparator;

public class PreciserComparator<T extends Preciser<? super T>>
		implements Comparator<T> {

	public static <T extends Preciser<? super T>> int comparePrecision( T one, T other ) {
		if ( one.morePreciseThan( other ) ) {
			return -1;
		}
		if ( other.morePreciseThan( one ) ) {
			return 1;
		}
		return 0;
	}

	@Override
	public int compare( T one, T other ) {
		return comparePrecision( one, other );
	}

}
