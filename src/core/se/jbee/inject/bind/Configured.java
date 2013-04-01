/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import static se.jbee.inject.Name.named;
import static se.jbee.inject.Name.namedInternal;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;

public final class Configured<T>
		implements Naming<T> {

	public static final Naming<Enum<?>> ENUM = new EnumNaming<Enum<?>>();
	public static final Naming<? super Object> TO_STRING = new ToStringNaming();

	public static <T extends Enum<?>> Configured<T> configured( Instance<T> value ) {
		return configured( ENUM, value );
	}

	public static <T> Configured<T> configured( Naming<? super T> naming, Instance<T> value ) {
		return new Configured<T>( value, naming );
	}

	private final Instance<T> value;
	private final Naming<? super T> naming;

	private Configured( Instance<T> value, Naming<? super T> naming ) {
		super();
		this.value = value;
		this.naming = naming;
	}

	public Instance<T> getInstance() {
		return value;
	}

	@Override
	public Name name( T value ) {
		return naming.name( value );
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
