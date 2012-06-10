package de.jbee.inject;

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

	public static <T> T newInstance( Class<T> type ) {
		try {
			return accessibleNoArgsConstructor( type ).newInstance();
		} catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

}
