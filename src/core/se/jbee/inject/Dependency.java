/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static se.jbee.inject.Instance.defaultInstanceOf;
import static se.jbee.inject.Type.raw;

import java.util.Arrays;
import java.util.Iterator;

import se.jbee.inject.UnresolvableDependency.DependencyCycle;
import se.jbee.inject.UnresolvableDependency.UnstableDependency;

/**
 * Describes what is wanted/needed as parameter to construct a instance of T.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Dependency<T>
		implements Parameter<T>, Iterable<Injection> {

	/**
	 * A empty {@link Injection} hierarchy. It is used whenever the {@link Dependency} does not
	 * depend on the actual hierarchy. This is the default.
	 */
	private static final Injection[] UNTARGETED = new Injection[0];

	@SuppressWarnings("rawtypes")
	public static Dependency<Class[]> pluginsFor(Class<?> pluginPoint) {
		return dependency( raw( Class[].class ).parametizedAsUpperBounds() ).named(pluginPoint.getCanonicalName()+":*");		
	}
	
	public static <T> Dependency<T> dependency( Class<T> type ) {
		return dependency( raw( type ) );
	}

	public static <T> Dependency<T> dependency( Type<T> type ) {
		return dependency( type, UNTARGETED );
	}

	private static <T> Dependency<T> dependency( Type<T> type, Injection[] hierarchy ) {
		return dependency( Instance.instance( Name.ANY, type ), hierarchy );
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

	public Instance<T> instance() {
		return instance;
	}

	@Override
	public Type<T> type() {
		return instance.type();
	}

	public Name name() {
		return instance.name;
	}

	@Override
	public String toString() {
		return instance.toString() + ( hierarchy.length == 0
			? ""
			: " " + Arrays.toString( hierarchy ) );
	}

	public Dependency<?> onTypeParameter() {
		return dependency( type().parameter( 0 ), hierarchy );
	}

	public <E> Dependency<E> instanced( Instance<E> instance ) {
		return dependency( instance, hierarchy );
	}

	@Override
	public <E> Dependency<E> typed( Type<E> type ) {
		return dependency( Instance.instance( name(), type ), hierarchy );
	}

	public <E> Dependency<E> anyTyped( Type<E> type ) {
		return dependency( Instance.instance( Name.ANY, type ), hierarchy );
	}

	public <E> Dependency<E> anyTyped( Class<E> type ) {
		return anyTyped( raw( type ) );
	}

	public Dependency<T> named( String name ) {
		return named( Name.named( name ) );
	}

	public Dependency<T> named( Name name ) {
		return dependency( Instance.instance( name, type() ), hierarchy );
	}

	public Dependency<T> untargeted() {
		return dependency( instance, UNTARGETED );
	}

	public Dependency<T> ignoredExpiry() {
		if ( hierarchy.length == 0 ) {
			return this;
		}
		Injection[] ignored = new Injection[hierarchy.length];
		for ( int i = 0; i < ignored.length; i++ ) {
			ignored[i] = hierarchy[i].ignoredExpiry();
		}
		return dependency( instance, ignored );
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
			: hierarchy[hierarchy.length - 1 - level].target.instance;
	}

	public int injectionDepth() {
		return hierarchy.length;
	}

	/**
	 * Means we inject into the argument target class.
	 */
	public Dependency<T> injectingInto( Class<?> target ) throws DependencyCycle, UnstableDependency {
		return injectingInto( raw( target ) );
	}

	public Dependency<T> injectingInto( Type<?> target ) throws DependencyCycle, UnstableDependency {
		return injectingInto( defaultInstanceOf( target ) );
	}

	public <I> Dependency<T> injectingInto( Instance<I> target ) throws DependencyCycle, UnstableDependency {
		return injectingInto( new Resource<I>(target), Expiry.NEVER );
	}

	public Dependency<T> injectingInto( Resource<?> target, Expiry expiry ) throws DependencyCycle, UnstableDependency {
		Injection injection = new Injection( instance, target, expiry );
		if ( hierarchy.length == 0 ) {
			return new Dependency<T>( instance, injection );
		}
		ensureNotMoreFrequentExpiry( injection );
		ensureNoCycle( injection );
		return new Dependency<T>( instance, Array.append( hierarchy, injection ) );
	}

	public Dependency<T> uninject() {
		if ( hierarchy.length <= 1 ) {
			return untargeted();
		}
		return new Dependency<T>( instance, Arrays.copyOf( hierarchy, hierarchy.length - 1 ) );
	}

	private void ensureNoCycle( Injection injection ) throws DependencyCycle {
		for ( int i = 0; i < hierarchy.length; i++ ) {
			Injection parent = hierarchy[i];
			if ( parent.equalTo( injection ) ) {
				throw new DependencyCycle( this, injection.target );
			}
		}
	}

	private void ensureNotMoreFrequentExpiry( Injection injection ) throws UnstableDependency {
		final Expiry expiry = injection.expiry;
		for ( int i = 0; i < hierarchy.length; i++ ) {
			Injection parent = hierarchy[i];
			if ( expiry.moreFrequent( parent.expiry ) ) {
				throw new UnstableDependency( parent, injection );
			}
		}
	}

	@Override
	public boolean isAssignableTo( Type<?> type ) {
		return type().isAssignableTo( type );
	}

	@Override
	public Iterator<Injection> iterator() {
		return Arrays.asList( hierarchy ).iterator();
	}
}
