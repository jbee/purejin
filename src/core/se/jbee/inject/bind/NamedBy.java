/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import static se.jbee.inject.Name.named;
import static se.jbee.inject.Name.namedInternal;
import se.jbee.inject.Name;

public final class NamedBy {

	public static final Naming<Enum<?>> ENUM = new EnumNaming<Enum<?>>();
	public static final Naming<? super Object> TO_STRING = new ToStringNaming();

	private NamedBy() {
		throw new UnsupportedOperationException( "util" );
	}

	private static final class EnumNaming<E extends Enum<?>>
			implements Naming<E> {

		EnumNaming() {
			//make visible
		}

		@Override
		public Name name( E value ) {
			return named( value );
		}

	}

	private static final class ToStringNaming
			implements Naming<Object> {

		ToStringNaming() {
			//make visible
		}

		@Override
		public Name name( Object value ) {
			return namedInternal( String.valueOf( value ) );
		}

	}
}
