package se.jbee.inject.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class Classification {

	public static boolean homogenous( Class<?> type ) {
		if ( type.isInterface() || type == Object.class ) {
			return true;
		}
		for ( Field f : type.getDeclaredFields() ) {
			if ( !Modifier.isStatic( f.getModifiers() ) ) {
				return false;
			}
		}
		for ( Constructor<?> c : type.getDeclaredConstructors() ) {
			if ( c.getParameterTypes().length > 0 ) {
				// maybe args are passed to super-type so we check this
				return homogenous( type.getSuperclass() );
			}
		}
		return true;
	}
}
