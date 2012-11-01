/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * <i>Has a {@link Type}, is typed</i>.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <T>
 *            The actual type ({@link Class})
 */
public interface Typed<T> {

	/**
	 * @return The {@link Type} of this object.
	 */
	Type<T> getType();

	/**
	 * @return This object with the given {@link Type}.
	 */
	<E> Typed<E> typed( Type<E> type );
}
