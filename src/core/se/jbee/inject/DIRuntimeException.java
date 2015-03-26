/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.util.Arrays;
import java.util.Collection;

/**
 * Base {@link RuntimeException} for all exceptions cennected to the dependency injection process
 * itself.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public class DIRuntimeException
		extends RuntimeException {

	public DIRuntimeException( String message ) {
		super( message );
	}

	@Override
	public String toString() {
		return getMessage();
	}

	/**
	 * A dependency cycle so that injection is not possible. Remove the cycle to resolve.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	public static final class DependencyCycleException
			extends DIRuntimeException {

		public DependencyCycleException( Dependency<?> dependency, Resource<?> cycleTarget ) {
			super( "Cycle detected: " + injectionStack( dependency ) + cycleTarget );
		}

	}

	public static String injectionStack( Dependency<?> dependency ) {
		StringBuilder b = new StringBuilder();
		for ( Injection i : dependency ) {
			b.append( i.target ).append( " -> " );
		}
		return b.toString();
	}

	/**
	 * It has been tried to inject a shorter living instance into one that will most likely outlive
	 * the injected one. This is considered to be unintentional. Use a indirection like a provider
	 * or services to resolve the problem.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	public static final class MoreFrequentExpiryException
			extends DIRuntimeException {

		public MoreFrequentExpiryException( Injection parent, Injection injection ) {
			super( "Cannot inject " + injection.target + " " + injection.expiry  + " into " + parent.target+" "+parent.expiry );
		}

	}

	/**
	 * An {@link Injector} couldn't find a {@link Resource} that matches a {@link Dependency} to
	 * resolve.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	public static final class NoSuchResourceException
			extends DIRuntimeException {

		public <T> NoSuchResourceException( Dependency<T> dependency, Injectron<T>[] available ) {
			super( "No resource for dependency: " + injectionStack( dependency )
					+ dependency.getInstance() + "\navailable are (for same raw type): "
					+ describe( available ) );
		}

		public NoSuchResourceException( Collection<Type<?>> types ) {
			super( "No resource for required type(s) " + types );
		}
	}

	public static String describe( Injectron<?>... injectrons ) {
		if ( injectrons == null || injectrons.length == 0 ) {
			return "none";
		}
		StringBuilder b = new StringBuilder();
		for ( Injectron<?> i : injectrons ) {
			b.append( '\n' ).append( i.getInfo().resource.toString() ).append( " defined " ).append( i.getInfo().source );
		}
		return b.toString();
	}

	/**
	 * A method has been described by its return and {@link Parameter} {@link Type}s (e.g. for use
	 * as factory or service) but such a method cannot be found. That usual means the defining class
	 * hasn't been bound correctly or the signature has changed.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	public static final class NoSuchFunctionException
			extends DIRuntimeException {

		public NoSuchFunctionException( Type<?> returnType, Type<?>... parameterTypes ) {
			super( returnType + ":" + Arrays.toString( parameterTypes ) );
		}
	}

}
