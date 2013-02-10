/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.jbee.inject.Array;
import se.jbee.inject.DeclarationType;
import se.jbee.inject.Expiry;
import se.jbee.inject.Precision;
import se.jbee.inject.Repository;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.Source;
import se.jbee.inject.Supplier;
import se.jbee.inject.util.Scoped;
import se.jbee.inject.util.Suppliable;
import se.jbee.inject.util.Classification;

public final class Link {

	public static final Linker<Suppliable<?>> BUILDIN = linker( defaultExpiration() );

	private static Linker<Suppliable<?>> linker( Map<Scope, Expiry> expiryByScope ) {
		return new SuppliableLinker( expiryByScope );
	}

	private static IdentityHashMap<Scope, Expiry> defaultExpiration() {
		IdentityHashMap<Scope, Expiry> map = new IdentityHashMap<Scope, Expiry>();
		map.put( Scoped.APPLICATION, Expiry.NEVER );
		map.put( Scoped.INJECTION, Expiry.expires( 1000 ) );
		map.put( Scoped.THREAD, Expiry.expires( 500 ) );
		map.put( Scoped.DEPENDENCY_TYPE, Expiry.NEVER );
		map.put( Scoped.TARGET_INSTANCE, Expiry.NEVER );
		return map;
	}

	private Link() {
		throw new UnsupportedOperationException( "util" );
	}

	private static class SuppliableLinker
			implements Linker<Suppliable<?>> {

		private final Map<Scope, Expiry> expiryByScope;

		SuppliableLinker( Map<Scope, Expiry> expiryByScope ) {
			super();
			this.expiryByScope = expiryByScope;
		}

		@Override
		public Suppliable<?>[] link( Inspector inspector, Module... modules ) {
			return link( disambiguate( bindingsFrom( modules, inspector ) ) );
		}

		private Suppliable<?>[] link( Binding<?>[] bindings ) {
			Map<Scope, Repository> repositories = initRepositories( bindings );
			Suppliable<?>[] suppliables = new Suppliable<?>[bindings.length];
			for ( int i = 0; i < bindings.length; i++ ) {
				Binding<?> binding = bindings[i];
				Scope scope = binding.scope;
				Expiry expiry = expiryByScope.get( scope );
				if ( expiry == null ) {
					expiry = Expiry.NEVER;
				}
				suppliables[i] = binding.suppliableIn( repositories.get( scope ), expiry );
			}
			return suppliables;
		}

		private Map<Scope, Repository> initRepositories( Binding<?>[] bindings ) {
			Map<Scope, Repository> repositories = new IdentityHashMap<Scope, Repository>();
			for ( Binding<?> i : bindings ) {
				Repository repository = repositories.get( i.scope );
				if ( repository == null ) {
					repositories.put( i.scope, i.scope.init() );
				}
			}
			return repositories;
		}

		/**
		 * Removes those bindings that are ambiguous but also do not clash because of different
		 * {@link DeclarationType}s that replace each other.
		 */
		private Binding<?>[] disambiguate( Binding<?>[] bindings ) {
			if ( bindings.length <= 1 ) {
				return bindings;
			}
			List<Binding<?>> res = new ArrayList<Binding<?>>( bindings.length );
			Arrays.sort( bindings );
			res.add( bindings[0] );
			int lastIndependend = 0;
			for ( int i = 1; i < bindings.length; i++ ) {
				Binding<?> one = bindings[lastIndependend];
				Binding<?> other = bindings[i];
				boolean equalResource = one.resource.equalTo( other.resource );
				if ( !equalResource || !other.source.getType().replacedBy( one.source.getType() ) ) {
					res.add( other );
					lastIndependend = i;
				} else if ( one.source.getType().clashesWith( other.source.getType() ) ) {
					throw new IllegalStateException( "Duplicate binds:\n" + one + "\n" + other );
				}
			}
			return Array.of( res, Binding.class );
		}

		private Binding<?>[] bindingsFrom( Module[] modules, Inspector inspector ) {
			Set<Class<?>> declared = new HashSet<Class<?>>();
			Set<Class<?>> multimodals = new HashSet<Class<?>>();
			ListBindings bindings = new ListBindings();
			for ( Module m : modules ) {
				Class<? extends Module> ns = m.getClass();
				final boolean hasBeenDeclared = declared.contains( ns );
				if ( hasBeenDeclared ) {
					if ( !Classification.monomodal( ns ) ) {
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

	}

	private static class ListBindings
			implements Bindings {

		final List<Binding<?>> list = new ArrayList<Binding<?>>( 100 );

		ListBindings() {
			// make visible
		}

		@Override
		public <T> void add( Resource<T> resource, Supplier<? extends T> supplier, Scope scope,
				Source source ) {
			list.add( new Binding<T>( resource, supplier, scope, source ) );
		}

	}

	private static final class Binding<T>
			implements Comparable<Binding<?>> {

		final Resource<T> resource;
		final Supplier<? extends T> supplier;
		final Scope scope;
		final Source source;

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

		Suppliable<T> suppliableIn( Repository repository, Expiry expiration ) {
			return new Suppliable<T>( resource, supplier, repository, expiration, source );
		}

	}
}
