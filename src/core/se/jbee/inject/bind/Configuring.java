/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import static se.jbee.inject.Name.namedInternal;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;

/**
 * A {@linkplain Configuring} value describes a value (bound within an {@link Injector}) that is used
 * to control configuration dependent implementation/injection (CDI). When CDI is used a type is
 * resolved to different named instances dependent on the {@linkplain Configuring} value. To derive
 * the name a actual value is associated with a {@link Naming} strategy is specified together with
 * the {@link Instance} of the controlling value.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <T>
 *            The type of the value that controlls the configuration
 */
public final class Configuring<T>
		implements Naming<T> {

	public static final Naming<Enum<?>> ENUM = new EnumNaming<Enum<?>>();
	public static final Naming<? super Object> TO_STRING = new ToStringNaming();

	public static <T extends Enum<?>> Configuring<T> configuring( Instance<T> value ) {
		return configuring( ENUM, value );
	}

	public static <T> Configuring<T> configuring( Naming<? super T> naming, Instance<T> value ) {
		return new Configuring<T>( value, naming );
	}

	private final Instance<T> value;
	private final Naming<? super T> naming;

	private Configuring( Instance<T> value, Naming<? super T> naming ) {
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
			return namedInternal( value );
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
