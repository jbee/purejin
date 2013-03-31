/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.Instance;
import se.jbee.inject.Name;

public final class Configured<T>
		implements Naming<T> {

	public static <T extends Enum<?>> Configured<T> configured( Instance<T> value ) {
		return configured( NamedBy.ENUM, value );
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
}
