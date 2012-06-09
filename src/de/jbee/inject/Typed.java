package de.jbee.inject;

public interface Typed<T> {

	Type<T> getType();

	<E> Typed<E> typed( Type<E> type );
}
