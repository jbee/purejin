/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package de.jbee.inject;

public interface Typed<T> {

	Type<T> getType();

	<E> Typed<E> typed( Type<E> type );
}
