/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.lang;

/**
 * <i>Has a</i> {@link Type}, {@link #typed(Type)} as...
 *
 * @author Jan Bernitt (jan@jbee.se)
 *
 * @param <T> The actual type ({@link Class})
 */
@SuppressWarnings("ClassReferencesSubclass")
public interface Typed<T> {

	/**
	 * @return The {@link Type} of this object.
	 */
	Type<T> type();

	/**
	 * @return This object with the given {@link Type}.
	 *
	 * @throws ClassCastException in case this cannot be typed as the type
	 *             given.
	 */
	<E> Typed<E> typed(Type<E> type);
}
