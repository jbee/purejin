/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.util.Arrays;
import java.util.Collection;

/**
 * Silks array util.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Array {

	public static <T> T[] insert( T[] array, T value, int index ) {
		if ( index < 0 ) {
			return append( array, value );
		}
		T[] copy = array.clone();
		copy[index] = value;
		return copy;
	}

	public static <T> T[] append( T[] array, T value ) {
		T[] copy = Arrays.copyOf( array, array.length + 1 );
		copy[array.length] = value;
		return copy;
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> T[] prepand( T value, T[] array ) {
		T[] copy = (T[]) newInstance( array.getClass().getComponentType(), array.length + 1 );
		System.arraycopy( array, 0, copy, 1, array.length );
		copy[0] = value;
		return copy;
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> T[] newInstance( Class<T> componentType, int length ) {
		return (T[]) java.lang.reflect.Array.newInstance( componentType, length );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> T[] newArrayInstance( Class<T[]> arrayType, int length ) {
		return (T[]) java.lang.reflect.Array.newInstance( arrayType.getComponentType(), length );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> T[] fill( T value, int length ) {
		T[] res = (T[]) newInstance( value.getClass(), length );
		for ( int i = 0; i < res.length; i++ ) {
			res[i] = value;
		}
		return res;
	}

	public static <T> T[] of( Collection<? extends T> list, Class<T> type ) {
		return list.toArray( newInstance( type, list.size() ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> T[] of( Collection<? extends T> list, T[] empty ) {
		if ( list.isEmpty() ) {
			return empty;
		}
		return of( list, (Class<T>) empty.getClass().getComponentType() );
	}

	public static <T> T[] copy( T[] list, int length ) {
		return Arrays.copyOf( list, length );
	}
}
