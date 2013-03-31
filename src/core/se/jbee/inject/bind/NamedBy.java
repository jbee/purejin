/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.Name;

public final class NamedBy {

	public static final Naming<Enum<?>> ENUM = new EnumNaming<Enum<?>>();

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
			return Name.named( value );
		}

	}
}
