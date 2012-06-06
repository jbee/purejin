package de.jbee.inject;

public interface Preciser<T extends Preciser<T>> {

	boolean morePreciseThan( T other );
}
