package de.jbee.inject.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;

public class TypeReflector {

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
		c.setAccessible( true );
		return c;
	}

	public static <T> Constructor<T> accessibleConstructor( Class<T> declaringClass ) {

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
		makeAccessible( c );
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

}
