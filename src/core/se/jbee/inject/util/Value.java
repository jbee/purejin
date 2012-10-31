/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.util;

import se.jbee.inject.Type;

public final class Value<T> {

	public static <T> Value<T> value( Type<T> type, T value ) {
		return new Value<T>( type, value );
	}

	private final Type<T> type;
	private final T value;

	private Value( Type<T> type, T value ) {
		super();
		this.type = type;
		this.value = value;
	}

	public Type<T> getType() {
		return type;
	}

	public T getValue() {
		return value;
	}

	@Override
	public String toString() {
		return type + " " + value;
	}
}
