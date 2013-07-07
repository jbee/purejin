/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.jbee.inject.Array;
import se.jbee.inject.DIRuntimeException;
import se.jbee.inject.DeclarationType;
import se.jbee.inject.Instance;
import se.jbee.inject.Precision;
import se.jbee.inject.Resource;
import se.jbee.inject.Resourced;
import se.jbee.inject.Scope;
import se.jbee.inject.Source;
import se.jbee.inject.Supplier;
import se.jbee.inject.Type;
import se.jbee.inject.Typed;

/**
 * Default data strature to represent a 4-tuple created from {@link Bindings}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <T>
 *            The type of the bound value (instance)
 */
public final class Binding<T>
		implements Comparable<Binding<?>>, Module, Typed<T>, Resourced<T> {

	public static <T> Binding<T> binding( Resource<T> resource, BindingType type,
			Supplier<? extends T> supplier, Scope scope, Source source ) {
		return new Binding<T>( resource, type, supplier, scope, source );
	}

	private final Resource<T> resource;
	public final BindingType type;
	public final Supplier<? extends T> supplier;
	public final Scope scope;
	public final Source source;

	private Binding( Resource<T> resource, BindingType type, Supplier<? extends T> supplier,
			Scope scope, Source source ) {
		super();
		this.resource = resource;
		this.type = type;
		this.supplier = supplier;
		this.scope = scope;
		this.source = source;
	}

	@Override
	public Resource<T> getResource() {
		return resource;
	}

	public Instance<T> getInstance() {
		return resource.getInstance();
	}

	@Override
	public Type<T> getType() {
		return getResource().getType();
	}

	@SuppressWarnings ( "unchecked" )
	@Override
	public <E> Binding<E> typed( Type<E> type ) {
		if ( !getType().isAssignableTo( type ) ) {
			throw new UnsupportedOperationException(); //TODO better exception
		}
		return new Binding<E>( resource.typed( type ), this.type, (Supplier<? extends E>) supplier,
				scope, source );
	}

	public Binding<T> suppliedBy( BindingType type, Supplier<? extends T> supplier ) {
		return new Binding<T>( resource, type, supplier, scope, source );
	}

	@Override
	public void declare( Bindings bindings ) {
		bindings.add( this );
	}

	@Override
	public int compareTo( Binding<?> other ) {
		int res = resource.getType().getRawType().getCanonicalName().compareTo(
				other.resource.getType().getRawType().getCanonicalName() );
		if ( res != 0 ) {
			return res;
		}
		res = Precision.comparePrecision( resource.getInstance(), other.resource.getInstance() );
		if ( res != 0 ) {
			return res;
		}
		res = Precision.comparePrecision( resource.getTarget(), other.resource.getTarget() );
		if ( res != 0 ) {
			return res;
		}
		res = Precision.comparePrecision( source, other.source );
		if ( res != 0 ) {
			return res;
		}
		return -1; // keep order
	}

	@Override
	public String toString() {
		return resource + " / " + scope + " / " + source;
	}

	/**
	 * Removes those bindings that are ambiguous but also do not clash because of different
	 * {@link DeclarationType}s that replace each other.
	 */
	public static Binding<?>[] disambiguate( Binding<?>[] bindings ) {
		if ( bindings.length <= 1 ) {
			return bindings;
		}
		List<Binding<?>> uniques = new ArrayList<Binding<?>>( bindings.length );
		Arrays.sort( bindings );
		uniques.add( bindings[0] );
		int lastDistinctIndex = 0;
		Set<Type<?>> required = new HashSet<Type<?>>();
		Set<Type<?>> nullified = new HashSet<Type<?>>();
		for ( int i = 1; i < bindings.length; i++ ) {
			Binding<?> one = bindings[lastDistinctIndex];
			Binding<?> other = bindings[i];
			final boolean equalResource = one.resource.equalTo( other.resource );
			DeclarationType oneType = one.source.getType();
			DeclarationType otherType = other.source.getType();
			if ( equalResource && oneType.clashesWith( otherType ) ) {
				throw new IllegalStateException( "Duplicate binds:\n" + one + "\n" + other );
			}
			if ( other.source.getType() == DeclarationType.REQUIRED ) {
				required.add( other.resource.getType() );
			} else if ( equalResource && oneType.nullifiedBy( otherType ) ) {
				if ( i - 1 == lastDistinctIndex ) {
					uniques.remove( uniques.size() - 1 );
					nullified.add( one.resource.getType() );
				}
			} else if ( !equalResource || !otherType.replacedBy( oneType ) ) {
				uniques.add( other );
				lastDistinctIndex = i;
			}
		}
		if ( required.isEmpty() ) {
			return Array.of( uniques, Binding.class );
		}
		Set<Type<?>> bound = new HashSet<Type<?>>();
		Set<Type<?>> provided = new HashSet<Type<?>>();
		for ( Binding<?> b : uniques ) {
			Type<?> type = b.resource.getType();
			if ( b.source.getType() == DeclarationType.PROVIDED ) {
				provided.add( type );
			} else {
				bound.add( type );
			}
		}
		required.removeAll( bound );
		if ( !provided.containsAll( required ) ) {
			required.removeAll( provided );
			throw new DIRuntimeException.NoSuchResourceException( required );
		}
		List<Binding<?>> res = new ArrayList<Binding<?>>( uniques.size() );
		for ( int i = 0; i < uniques.size(); i++ ) {
			Binding<?> b = uniques.get( i );
			if ( b.source.getType() != DeclarationType.PROVIDED
					|| required.contains( b.resource.getType() ) ) {
				res.add( b );
			}
		}
		return Array.of( res, Binding.class );
	}
}