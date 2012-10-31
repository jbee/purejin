/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import se.jbee.inject.Type;

public final class TypeReflector {

	private TypeReflector() {
		throw new UnsupportedOperationException( "util" );
	}

	public static <T> Constructor<T> accessibleNoArgsConstructor( Class<T> declaringClass ) {
		if ( declaringClass.isInterface() ) {
			throw new IllegalArgumentException( "Interfaces don't have constructors: "
					+ declaringClass );
		}
		Constructor<T> c;
		try {
			c = declaringClass.getDeclaredConstructor( new Class<?>[0] );
		} catch ( Exception e ) {
			throw new RuntimeException( e );
		}
		makeAccessible( c );
		return c;
	}

	public static <T> Constructor<T> defaultConstructor( Class<T> declaringClass ) {

		Constructor<?>[] constructors = declaringClass.getDeclaredConstructors();
		if ( constructors.length == 0 ) {
			throw new RuntimeException( new NoSuchMethodException(
					declaringClass.getCanonicalName() ) );
		}
		int noArgsIndex = 0;
		for ( int i = 0; i < constructors.length; i++ ) {
			if ( constructors[i].getParameterTypes().length == 0 ) {
				noArgsIndex = i;
			}
		}
		@SuppressWarnings ( "unchecked" )
		Constructor<T> c = (Constructor<T>) constructors[noArgsIndex];
		return c;
	}

	public static <T> T newInstance( Class<T> type ) {
		try {
			return accessibleNoArgsConstructor( type ).newInstance();
		} catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	public static void makeAccessible( AccessibleObject obj ) {
		obj.setAccessible( true );
	}

	public static <T> T construct( Constructor<T> constructor, Object... args ) {
		try {
			return constructor.newInstance( args );
		} catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	public static Object invoke( Method method, Object owner, Object... args ) {
		try {
			return method.invoke( owner, args );
		} catch ( Exception e ) {
			if ( e instanceof InvocationTargetException ) {
				Throwable t = ( (InvocationTargetException) e ).getTargetException();
				if ( t instanceof Exception ) {
					e = (Exception) t;
				}
			}
			throw new RuntimeException( "Failed to invoke method: " + method + " \n"
					+ e.getMessage(), e );
		}
	}

	public static <T> Method methodReturns( Type<T> returnType, Class<?> implementor ) {
		for ( Method m : implementor.getDeclaredMethods() ) {
			if ( Type.returnType( m ).isAssignableTo( returnType ) ) {
				return m;
			}
		}
		return null;
	}

}
