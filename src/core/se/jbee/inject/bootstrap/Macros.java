/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import se.jbee.inject.Type;

public final class Macros {

	public static Macros macros( Macro<?>... macros ) {
		Class<?>[] types = new Class<?>[macros.length];
		Macro<?>[] mcs = new Macro<?>[macros.length];
		for ( int i = 0; i < macros.length; i++ ) {
			types[i] = Type.supertype( Macro.class, Type.raw( macros[i].getClass() ) ).parameter( 0 ).getRawType();
			mcs[i] = macros[i];
		}
		return new Macros( types, mcs );
	}

	private final Class<?>[] types;
	private final Macro<?>[] macros;

	private Macros( Class<?>[] types, Macro<?>[] macros ) {
		super();
		this.types = types;
		this.macros = macros;
	}

	public <T, V> Module expand( Binding<T> binding, V value ) {
		return macro( value ).expand( binding, value );
	}

	@SuppressWarnings ( "unchecked" )
	private <V> Macro<V> macro( V value ) {
		final Class<?> type = value.getClass();
		for ( int i = 0; i < types.length; i++ ) {
			if ( types[i] == type ) {
				return (Macro<V>) macros[i];
			}
		}
		throw new IllegalArgumentException( type.getCanonicalName() );
	}

}
