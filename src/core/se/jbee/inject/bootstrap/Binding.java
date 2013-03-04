/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.util.Metaclass.metaclass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.jbee.inject.Array;
import se.jbee.inject.DeclarationType;
import se.jbee.inject.Precision;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.Source;
import se.jbee.inject.Supplier;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Link.ListBindings;

/**
 * Default data strature to represent a 4-tuple created from {@link Bindings}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <T>
 *            The type of the bound value (instance)
 */
public final class Binding<T>
		implements Comparable<Binding<?>> {

	public final Resource<T> resource;
	public final Supplier<? extends T> supplier;
	public final Scope scope;
	public final Source source;

	Binding( Resource<T> resource, Supplier<? extends T> supplier, Scope scope, Source source ) {
		super();
		this.resource = resource;
		this.supplier = supplier;
		this.scope = scope;
		this.source = source;
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

	public static Binding<?>[] expand( Inspector inspector, Module... modules ) {
		Set<Class<?>> declared = new HashSet<Class<?>>();
		Set<Class<?>> multimodals = new HashSet<Class<?>>();
		ListBindings bindings = new ListBindings();
		for ( Module m : modules ) {
			Class<? extends Module> ns = m.getClass();
			final boolean hasBeenDeclared = declared.contains( ns );
			if ( hasBeenDeclared ) {
				if ( !metaclass( ns ).monomodal() ) {
					multimodals.add( ns );
				}
			}
			if ( !hasBeenDeclared || multimodals.contains( ns ) ) {
				m.declare( bindings, inspector );
				declared.add( ns );
			}
		}
		return Array.of( bindings.list, Binding.class );
	}

	/**
	 * Removes those bindings that are ambiguous but also do not clash because of different
	 * {@link DeclarationType}s that replace each other.
	 */
	public static Binding<?>[] disambiguate( Binding<?>[] bindings ) {
		if ( bindings.length <= 1 ) {
			return bindings;
		}
		List<Binding<?>> res = new ArrayList<Binding<?>>( bindings.length );
		Arrays.sort( bindings );
		res.add( bindings[0] );
		int lastDistinctIndex = 0;
		Set<Type<?>> provided = new HashSet<Type<?>>();
		provided.add( bindings[0].resource.getType() );
		Set<Type<?>> required = new HashSet<Type<?>>();
		for ( int i = 1; i < bindings.length; i++ ) {
			Binding<?> one = bindings[lastDistinctIndex];
			Binding<?> other = bindings[i];
			boolean equalResource = one.resource.equalTo( other.resource );
			if ( equalResource && one.source.getType().clashesWith( other.source.getType() ) ) {
				throw new IllegalStateException( "Duplicate binds:\n" + one + "\n" + other );
			}
			if ( other.source.getType() == DeclarationType.REQUIRED ) {
				required.add( other.resource.getType() );
			} else if ( !equalResource || !other.source.getType().replacedBy( one.source.getType() ) ) {
				res.add( other );
				provided.add( other.resource.getType() );
				lastDistinctIndex = i;
			}
		}
		if ( !provided.containsAll( required ) ) {
			throw new IllegalStateException( "Missing required type(s)" );
		}
		return Array.of( res, Binding.class );
	}

}