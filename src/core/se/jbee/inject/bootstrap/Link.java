/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import java.util.IdentityHashMap;
import java.util.Map;

import se.jbee.inject.Expiry;
import se.jbee.inject.Repository;
import se.jbee.inject.Scope;
import se.jbee.inject.util.Scoped;
import se.jbee.inject.util.Suppliable;

/**
 * Default implementation of the {@link Linker} that creates {@link Suppliable}s.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
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
		map.put( Scoped.DEPENDENCY, Expiry.NEVER );
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
		public Suppliable<?>[] link( Macros macros, Inspector inspector, Module... modules ) {
			return link( Binding.disambiguate( Bindings.expand( macros, inspector, modules ) ) );
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
				suppliables[i] = suppliableOf( binding, repositories.get( scope ), expiry );
			}
			return suppliables;
		}

		private static <T> Suppliable<T> suppliableOf( Binding<T> binding, Repository repository,
				Expiry expiration ) {
			return new Suppliable<T>( binding.resource, binding.supplier, repository, expiration,
					binding.source );
		}

		private static Map<Scope, Repository> initRepositories( Binding<?>[] bindings ) {
			Map<Scope, Repository> repositories = new IdentityHashMap<Scope, Repository>();
			for ( Binding<?> i : bindings ) {
				Repository repository = repositories.get( i.scope );
				if ( repository == null ) {
					repositories.put( i.scope, i.scope.init() );
				}
			}
			return repositories;
		}

	}

}
