/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import se.jbee.inject.UnresolvableDependency.SupplyFailed;

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

	public static <T> T constructor( Constructor<T> constructor, Object... args ) throws SupplyFailed {
		try {
			return constructor.newInstance( args );
		} catch ( Exception e ) {
			throw SupplyFailed.valueOf(e, constructor);
		}
	}

	public static Object method( Method method, Object owner, Object... args ) throws SupplyFailed {
		try {
			return method.invoke( owner, args );
		} catch ( Exception e ) {
			throw SupplyFailed.valueOf(e, method);
		}
	}

}
