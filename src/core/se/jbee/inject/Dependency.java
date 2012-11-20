/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static se.jbee.inject.Emergence.emergence;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Type.raw;

import java.util.Arrays;
import java.util.Iterator;

import se.jbee.inject.DIRuntimeException.DependencyCycleException;
import se.jbee.inject.DIRuntimeException.MoreFrequentExpiryException;

/**
 * Describes what is wanted/needed as parameter to construct a instance of T.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public final class Dependency<T>
		implements Typed<T>, Named, Parameter<T>, Iterable<Injection> {

	private static final Injection[] UNTARGETED = new Injection[0];

	public static <T> Dependency<T> dependency( Class<T> type ) {
		return dependency( raw( type ) );
	}

	public static <T> Dependency<T> dependency( Type<T> type ) {
		return dependency( type, UNTARGETED );
	}

	private static <T> Dependency<T> dependency( Type<T> type, Injection[] hierarchy ) {
		return dependency( instance( Name.ANY, type ), hierarchy );
	}

	public static <T> Dependency<T> dependency( Instance<T> instance ) {
		return dependency( instance, UNTARGETED );
	}

	private static <T> Dependency<T> dependency( Instance<T> instance, Injection[] hierarchy ) {
		return new Dependency<T>( instance, hierarchy );
	}

	private final Injection[] hierarchy;
	private final Instance<T> instance;

	private Dependency( Instance<T> instance, Injection... hierarchy ) {
		this.instance = instance;
		this.hierarchy = hierarchy;
	}

	public Instance<T> getInstance() {
		return instance;
	}

	@Override
	public Type<T> getType() {
		return instance.getType();
	}

	@Override
	public Name getName() {
		return instance.getName();
	}

	@Override
	public String toString() {
		return instance.toString() + ( hierarchy.length == 0
			? ""
			: " " + Arrays.toString( hierarchy ) );
	}

	public Dependency<?> onTypeParameter() {
		return dependency( getType().getParameters()[0], hierarchy );
	}

	public <E> Dependency<E> instanced( Instance<E> instance ) {
		return dependency( instance, hierarchy );
	}

	@Override
	public <E> Dependency<E> typed( Type<E> type ) {
		return dependency( instance( getName(), type ), hierarchy );
	}

	public <E> Dependency<E> anyTyped( Type<E> type ) {
		return dependency( instance( Name.ANY, type ), hierarchy );
	}

	public <E> Dependency<E> anyTyped( Class<E> type ) {
		return anyTyped( raw( type ) );
	}

	public Dependency<T> named( String name ) {
		return named( Name.named( name ) );
	}

	public Dependency<T> named( Name name ) {
		return dependency( instance( name, getType() ), hierarchy );
	}

	public Dependency<T> untargeted() {
		return dependency( instance, UNTARGETED );
	}

	public boolean isUntargeted() {
		return hierarchy.length == 0;
	}

	public Instance<?> target() {
		return target( 0 );
	}

	public Instance<?> target( int level ) {
		return isUntargeted()
			? Instance.ANY
			: hierarchy[hierarchy.length - 1 - level].getTarget().getInstance();
	}

	public int injectionDepth() {
		return hierarchy.length;
	}

	/**
	 * Means we inject into the argument target class.
	 */
	public Dependency<T> injectingInto( Class<?> target ) {
		return injectingInto( raw( target ) );
	}

	public Dependency<T> injectingInto( Type<?> target ) {
		return injectingInto( Instance.defaultInstanceOf( target ) );
	}

	public Dependency<T> injectingInto( Instance<?> target ) {
		return injectingInto( emergence( target, Expiry.NEVER ) );
	}

	public Dependency<T> injectingInto( Emergence<?> target ) {
		Injection injection = new Injection( instance, target );
		if ( hierarchy.length == 0 ) {
			return new Dependency<T>( instance, injection );
		}
		ensureNotMoreFrequentExpiry( injection );
		ensureNoCycle( injection );
		Injection[] copy = Arrays.copyOf( hierarchy, hierarchy.length + 1 );
		copy[hierarchy.length] = injection;
		return new Dependency<T>( instance, copy );
	}

	private void ensureNoCycle( Injection injection )
			throws DependencyCycleException {
		for ( int i = 0; i < hierarchy.length; i++ ) {
			Injection parent = hierarchy[i];
			if ( parent.equalTo( injection ) ) {
				throw new DependencyCycleException( this, injection.getTarget().getInstance() );
			}
		}
	}

	private void ensureNotMoreFrequentExpiry( Injection injection ) {
		final Expiry expiry = injection.getTarget().getExpiry();
		for ( int i = 0; i < hierarchy.length; i++ ) {
			Injection parent = hierarchy[i];
			if ( expiry.moreFrequent( parent.getTarget().getExpiry() ) ) {
				throw new MoreFrequentExpiryException( parent, injection );
			}
		}
	}

	@Override
	public boolean isAssignableTo( Type<?> type ) {
		return getType().isAssignableTo( type );
	}

	@Override
	public Iterator<Injection> iterator() {
		return Arrays.asList( hierarchy ).iterator();
	}
}
