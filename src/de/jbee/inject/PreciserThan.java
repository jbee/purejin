package de.jbee.inject;

public interface PreciserThan<T extends PreciserThan<T>> {

	boolean morePreciseThan( T other );
}
