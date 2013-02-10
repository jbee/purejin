package se.jbee.inject.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class Classification {

	/**
	 * A {@link Class} is monomodal if it there is just a single possible initial state. All newly
	 * created instances can just have this similar initial state but due to internal state they
	 * could develop (behave) different later on.
	 * 
	 * The opposite of monomodal is multimodal.
	 */
	public static boolean monomodal( Class<?> type ) {
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
				return monomodal( type.getSuperclass() );
			}
		}
		return true;
	}

	//monovalent
}
