/*
 *  Copyright (c) 2012-2017, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import se.jbee.inject.bootstrap.Binding;

/**
 * A {@link Dependency} that should be resolved during the injection or
 * resolution process.
 * 
 * This is a grouping {@link RuntimeException} the problem specific exceptions
 * are derived from.
 * 
 * @see DependencyCycle
 * @see UnstableDependency
 * @see NoResourceForDependency
 * @see NoMethodForDependency
 * @see SupplyFailed
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class UnresolvableDependency
		extends RuntimeException {

	protected UnresolvableDependency( String message ) {
		super( message );
	}

	protected UnresolvableDependency(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public String toString() {
		return getMessage();
	}

	/**
	 * A dependency cycle so that injection is not possible. Remove the cycle to resolve.
	 */
	public static final class DependencyCycle
			extends UnresolvableDependency {

		public DependencyCycle( Dependency<?> dependency, Resource<?> cycleTarget ) {
			super( "Cycle detected: " +  dependency + cycleTarget );
		}

	}

	/**
	 * It has been tried to inject a shorter living instance into one that will most likely outlive
	 * the injected one. This is considered to be unintentional. Use a indirection like a provider
	 * or services to resolve the problem.
	 */
	public static final class UnstableDependency
			extends UnresolvableDependency {

		public UnstableDependency( Injection parent, Injection injection ) {
			super( "Cannot inject " + injection.target + " " + injection.expiry  + " into " + parent.target+" "+parent.expiry );
		}

	}

	/**
	 * An {@link Injector} couldn't find a {@link Resource} that matches a {@link Dependency} to
	 * resolve.
	 */
	public static final class NoResourceForDependency
			extends UnresolvableDependency {

		public <T> NoResourceForDependency( Dependency<T> dependency, Injectron<T>[] available, String msg ) {
			super( "No resource for dependency:\n" + dependency + "\navailable are (for same raw type): "
					+ describe( available ) + "\n"
					+ msg);
		}

		public NoResourceForDependency( Collection<Type<?>> types, List<?> dropped ) {
			super( "No resource for required type(s): " + types+"\ndrobbed bindings:\n"+dropped );
		}
	}

	public static String describe( Injectron<?>... injectrons ) {
		if ( injectrons == null || injectrons.length == 0 ) {
			return "none";
		}
		StringBuilder b = new StringBuilder();
		for ( Injectron<?> i : injectrons ) {
			b.append( '\n' ).append( i.info().resource.toString() ).append( " defined " ).append( i.info().source );
		}
		return b.toString();
	}

	/**
	 * A method has been described by its return and {@link Parameter} {@link Type}s (e.g. for use
	 * as factory or service) but such a method cannot be found. That usual means the defining class
	 * hasn't been bound correctly or the signature has changed.
	 */
	public static final class NoMethodForDependency extends UnresolvableDependency {

		public NoMethodForDependency( Type<?> returnType, Type<?>... parameterTypes ) {
			super( returnType + ":" + Arrays.toString( parameterTypes ) );
		}
	}
	
	/**
	 * When trying to supply a {@link Dependency} (e.g. by calling a method or
	 * constructor) a error occurred that prevented a successful supply.
	 */
	public static final class SupplyFailed extends UnresolvableDependency {

		public SupplyFailed(String msg, Throwable cause) {
			super(msg, cause);
		}
		
		public static SupplyFailed valueOf(Exception e, AccessibleObject invoked) {
			if ( e instanceof InvocationTargetException ) {
				Throwable t = ( (InvocationTargetException) e ).getTargetException();
				if ( t instanceof Exception ) {
					e = (Exception) t;
				}
			}
			return new SupplyFailed("Failed to invoke: "+invoked, e );
		}		
	}

}