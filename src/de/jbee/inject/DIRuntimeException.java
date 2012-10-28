/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package de.jbee.inject;

import java.util.Arrays;

public class DIRuntimeException
		extends RuntimeException {

	public DIRuntimeException( String message ) {
		super( message );
	}

	@Override
	public String toString() {
		return getMessage();
	}

	public static final class DependencyCycleException
			extends DIRuntimeException {

		public DependencyCycleException( Dependency<?> dependency, Instance<?> cycleTarget ) {
			super( "Cycle detected: " + injectionStack( dependency ) + cycleTarget );
		}

	}

	static String injectionStack( Dependency<?> dependency ) {
		StringBuilder b = new StringBuilder();
		for ( Injection i : dependency ) {
			b.append( i.getTarget().getInstance() ).append( " -> " );
		}
		return b.toString();
	}

	public static final class MoreFrequentExpiryException
			extends DIRuntimeException {

		public MoreFrequentExpiryException( Injection parent, Injection injection ) {
			super( "Cannot inject " + injection.getTarget() + " into " + parent.getTarget() );
		}

	}

	/**
	 * An {@link Injector} couldn't find a {@link Resource} that matches a {@link Dependency} to
	 * resolve.
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 * 
	 */
	public static final class NoSuchResourceException
			extends DIRuntimeException {

		public <T> NoSuchResourceException( Dependency<T> dependency, Injectron<T>[] available ) {
			super( "No resource for dependency: " + injectionStack( dependency )
					+ dependency.getInstance() );
		}

	}

	public static final class NoSuchMethodException
			extends DIRuntimeException {

		public NoSuchMethodException( Type<?> returnType, Type<?>... parameterTypes ) {
			super( returnType + ":" + Arrays.toString( parameterTypes ) );
		}
	}
}
