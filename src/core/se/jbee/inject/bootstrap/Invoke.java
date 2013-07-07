/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A util to invoke {@link Constructor}s or {@link Method}s that converts checked {@link Exception}s
 * into {@link RuntimeException}s.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Invoke {

	private Invoke() {
		throw new UnsupportedOperationException( "util" );
	}

	public static <T> T constructor( Constructor<T> constructor, Object... args ) {
		try {
			return constructor.newInstance( args );
		} catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	public static Object method( Method method, Object owner, Object... args ) {
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

}
