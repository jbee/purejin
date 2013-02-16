/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

import java.util.IdentityHashMap;

import se.jbee.inject.Type;

/**
 * {@link Presets} are an immutable associative data structure associating a exact {@link Type}
 * (including generics) with a value for/of that exact given type. These values act as input
 * <i>parameters</i> to the bootstrapping process. The values are used within modules that depend on
 * data that is given as program input.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Presets {

	public static final Presets EMPTY = new Presets( new IdentityHashMap<String, Object>( 0 ) );

	private final IdentityHashMap<String, Object> values;

	private Presets( IdentityHashMap<String, Object> values ) {
		super();
		this.values = values;
	}

	/**
	 * @see #preset(Type, Object)
	 */
	public <T> Presets preset( Class<T> type, T value ) {
		return preset( Type.raw( type ), value );
	}

	/**
	 * @return new {@link Presets} instance with the given key-value association. Any existing
	 *         association of the same key will be overridden.
	 */
	public <T> Presets preset( Type<T> type, T value ) {
		final String key = key( type );
		if ( value == null && !values.containsKey( key ) ) {
			return this;
		}
		@SuppressWarnings ( "unchecked" )
		IdentityHashMap<String, Object> clone = (IdentityHashMap<String, Object>) values.clone();
		if ( value == null ) {
			clone.remove( key );
		} else {
			clone.put( key, value );
		}
		return new Presets( clone );
	}

	/**
	 * @return The value associated with the given exact {@link Type} or <code>null</code> of no
	 *         value is associated with it.
	 */
	@SuppressWarnings ( "unchecked" )
	public <T> T value( Type<T> type ) {
		return (T) values.get( key( type ) );
	}

	private static <T> String key( Type<T> type ) {
		return type.toString().intern();
	}

	@Override
	public String toString() {
		return values.toString();
	}
}
