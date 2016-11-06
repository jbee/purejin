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
import se.jbee.inject.DeclarationType;
import se.jbee.inject.InconsistentBinding;
import se.jbee.inject.Instance;
import se.jbee.inject.Resource;
import se.jbee.inject.Source;
import se.jbee.inject.Supplier;
import se.jbee.inject.Type;
import se.jbee.inject.Typed;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.container.Assembly;
import se.jbee.inject.container.Scope;

/**
 * Default data strature to represent a 4-tuple created from {@link Bindings}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <T>
 *            The type of the bound value (instance)
 */
public final class Binding<T>
		implements Comparable<Binding<?>>, Assembly<T>, Module, Typed<T> {

	public static <T> Binding<T> binding( Resource<T> resource, BindingType type,
			Supplier<? extends T> supplier, Scope scope, Source source ) {
		return new Binding<>( resource, type, supplier, scope, source );
	}

	public final Resource<T> resource;
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
	public Resource<T> resource() {
		return resource;
	}
	
	@Override
	public Scope scope() {
		return scope;
	}
	
	@Override
	public Source source() {
		return source;
	}
	
	@Override
	public Supplier<? extends T> supplier() {
		return supplier;
	}
	
	@Override
	public Type<T> type() {
		return resource.type();
	}

	@SuppressWarnings ( "unchecked" )
	@Override
	public <E> Binding<E> typed( Type<E> type ) {
		return new Binding<>( resource.typed( type().toSupertype(type) ), this.type, (Supplier<? extends E>) supplier,	scope, source );
	}

	public boolean isComplete() {
		return supplier != null;
	}
	
	public Binding<T> complete( BindingType type, Supplier<? extends T> supplier ) {
		return new Binding<>( resource, type, supplier, scope, source );
	}

	@Override
	public void declare( Bindings bindings ) {
		bindings.add( this );
	}

	@Override
	public int compareTo( Binding<?> other ) {
		int res = resource.type().rawType.getCanonicalName().compareTo(
				other.resource.type().rawType.getCanonicalName() );
		if ( res != 0 ) {
			return res;
		}
		res = Instance.comparePrecision( resource.instance, other.resource.instance );
		if ( res != 0 ) {
			return res;
		}
		res = Instance.comparePrecision( resource.target, other.resource.target );
		if ( res != 0 ) {
			return res;
		}
		res = Instance.comparePrecision( source, other.source );
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
		List<Binding<?>> uniques = new ArrayList<>( bindings.length );
		Arrays.sort( bindings );
		uniques.add( bindings[0] );
		int lastUniqueIndex = 0;
		Set<Type<?>> required = new HashSet<>();
		List<Binding<?>> dropped = new ArrayList<>(); 
		for ( int i = 1; i < bindings.length; i++ ) {
			Binding<?> b_d = bindings[lastUniqueIndex];
			Binding<?> b_i = bindings[i];
			final boolean equalResource = b_d.resource.equalTo( b_i.resource );
			DeclarationType t_d = b_d.source.declarationType;
			DeclarationType t_i = b_i.source.declarationType;
			if ( equalResource && t_d.clashesWith( t_i ) ) {
				throw new InconsistentBinding( "Duplicate binds:\n" + b_d + "\n" + b_i );
			}
			if ( t_i == DeclarationType.REQUIRED ) {
				required.add( b_i.resource.type() );
			} else if ( equalResource && t_d.droppedWith( t_i ) ) {
				if ( i - 1 == lastUniqueIndex ) {
					dropped.add(uniques.remove( uniques.size() - 1 ));
				}
				dropped.add(b_i);
			} else if ( !equalResource || !t_i.replacedBy( t_d ) ) {
				uniques.add( b_i );
				lastUniqueIndex = i;
			}
		}
		return withoutProvidedThatAreNotRequiredIn(uniques, required, dropped);
	}

	private static Binding<?>[] withoutProvidedThatAreNotRequiredIn(List<Binding<?>> bindings, Set<Type<?>> required, List<Binding<?>> dropped) {
		List<Binding<?>> res = new ArrayList<>( bindings.size() );
		for ( Binding<?> b : bindings ) {
			Type<?> type = b.resource.type();
			if ( b.source.declarationType != DeclarationType.PROVIDED || required.contains(type) ) {
				res.add( b );
				required.remove(type);
			}
		}
		if (!required.isEmpty() ) {
			throw new UnresolvableDependency.NoResourceForDependency( required, dropped );
		}
		return Array.of( res, Binding.class );
	}

}