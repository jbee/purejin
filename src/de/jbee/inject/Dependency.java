package de.jbee.inject;

import static de.jbee.inject.Instance.instance;
import static de.jbee.inject.Type.raw;

import java.util.Arrays;

/**
 * Describes what is wanted/needed as parameter to construct a instance of T.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <T>
 */
public final class Dependency<T>
		implements Typed<T>, Named {

	private static final Type<?>[] UNTARGETED = new Type<?>[0];

	public static <T> Dependency<T> dependency( Class<T> type ) {
		return dependency( raw( type ) );
	}

	public static <T> Dependency<T> dependency( Type<T> type ) {
		return dependency( type, UNTARGETED );
	}

	private static <T> Dependency<T> dependency( Type<T> type, Type<?>[] targetHierarchy ) {
		return dependency( instance( Name.ANY, type ), targetHierarchy );
	}

	private static <T> Dependency<T> dependency( Instance<T> instance, Type<?>[] targetHierarchy ) {
		return new Dependency<T>( instance, targetHierarchy );
	}

	private final Type<?>[] targetHierarchy;
	private final Instance<T> instance;

	private Dependency( Instance<T> instance, Type<?>[] targetHierarchy ) {
		this.instance = instance;
		this.targetHierarchy = targetHierarchy;
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
		return instance.toString();
	}

	public Dependency<?> onTypeParameter() {
		return dependency( getType().getParameters()[0], targetHierarchy );
	}

	@Override
	public <E> Dependency<E> typed( Type<E> type ) {
		return dependency( instance( getName(), type ), targetHierarchy );
	}

	public <E> Dependency<E> anyTyped( Type<E> type ) {
		return dependency( instance( Name.ANY, type ), targetHierarchy );
	}

	public <E> Dependency<E> anyTyped( Class<E> type ) {
		return anyTyped( raw( type ) );
	}

	public Dependency<T> named( String name ) {
		return named( Name.named( name ) );
	}

	public Dependency<T> named( Name name ) {
		return dependency( instance( name, getType() ), targetHierarchy );
	}

	public Dependency<T> untargeted() {
		return dependency( instance, UNTARGETED );
	}

	public boolean isUntargeted() {
		return targetHierarchy.length == 0;
	}

	public Type<?> target() {
		return isUntargeted()
			? Type.WILDCARD
			: targetHierarchy[targetHierarchy.length - 1];
	}

	/**
	 * Means we inject into the argument target class.
	 */
	public Dependency<T> injectingInto( Class<?> target ) {
		return injectingInto( raw( target ) );
	}

	public Dependency<T> injectingInto( Type<?> target ) {
		if ( targetHierarchy.length == 0 ) {
			return new Dependency<T>( instance, new Type<?>[] { target } );
		}
		Type<?>[] hierarchy = Arrays.copyOf( targetHierarchy, targetHierarchy.length + 1 );
		hierarchy[targetHierarchy.length] = target;
		return new Dependency<T>( instance, hierarchy );
	}

}
