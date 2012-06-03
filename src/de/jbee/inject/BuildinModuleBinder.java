/**
 * 
 */
package de.jbee.inject;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class BuildinModuleBinder
		implements ModuleBinder {

	// Find the initial set of bindings
	// 0. create BindInstruction
	// 2. sort instructions
	// 3. remove duplicates (implicit will be sorted after explicit)
	// 4. detect ambiguous bindings (two explicit bindings that have same type and availability)

	// 1. Create Scope-Repositories
	//   a. sort scopes from most stable to most fragile
	// 	 b. init one repository for each scope
	// 	 c. apply snapshots wrapper to repository instances
	@Override
	public Binding<?>[] bind( Module root ) {
		return bind( cleanedUp( declarationsFrom( root ) ) );
	}

	private Binding<?>[] bind( BindDeclaration<?>[] declarations ) {
		Map<Scope, Repository> repositories = buildRepositories( declarations );
		Binding<?>[] bindings = new Binding<?>[declarations.length];
		for ( int i = 0; i < declarations.length; i++ ) {
			BindDeclaration<?> instruction = declarations[i];
			bindings[i] = instruction.toBinding( repositories.get( instruction.scope() ) );
		}
		return bindings;
	}

	private Map<Scope, Repository> buildRepositories( BindDeclaration<?>[] instructions ) {
		Map<Scope, Repository> repositories = new IdentityHashMap<Scope, Repository>();
		for ( BindDeclaration<?> i : instructions ) {
			Repository repository = repositories.get( i.scope() );
			if ( repository == null ) {
				repositories.put( i.scope(), i.scope().init( instructions.length ) );
			}
		}
		return repositories;
	}

	private BindDeclaration<?>[] cleanedUp( BindDeclaration<?>[] instructions ) {
		Arrays.sort( instructions );

		return instructions;
	}

	private BindDeclaration<?>[] declarationsFrom( Module root ) {
		DeclarationBinder binder = new DeclarationBinder();
		root.configure( binder );
		return binder.declarations.toArray( new BindDeclaration<?>[0] );
	}

	static class DeclarationBinder
			implements Binder {

		final List<BindDeclaration<?>> declarations = new LinkedList<BindDeclaration<?>>();

		@Override
		public <T> void bind( Resource<T> resource, Supplier<? extends T> supplier, Scope scope,
				Source source ) {
			declarations.add( new BindDeclaration<T>( declarations.size(), resource, supplier, scope,
					source ) );
		}

	}

	static final class BindDeclaration<T>
			implements Comparable<BindDeclaration<?>> {

		private final int nr;
		private final Resource<T> resource;
		private final Supplier<? extends T> supplier;
		private final Scope scope;
		private final Source source;

		BindDeclaration( int nr, Resource<T> resource, Supplier<? extends T> supplier, Scope scope,
				Source source ) {
			super();
			this.nr = nr;
			this.resource = resource;
			this.supplier = supplier;
			this.scope = scope;
			this.source = source;
		}

		Resource<T> resource() {
			return resource;
		}

		Supplier<? extends T> supplier() {
			return supplier;
		}

		Scope scope() {
			return scope;
		}

		Source source() {
			return source;
		}

		Binding<T> toBinding( Repository repository ) {
			return new Binding<T>( resource, supplier, repository, source );
		}

		@Override
		public int compareTo( BindDeclaration<?> other ) {
			int res = resource.getType().compareTo( other.resource.getType() );
			if ( res != 0 ) {
				return res;
			}
			if ( resource.getName().morePreciseThan( other.resource.getName() ) ) {
				return 1;
			}
			if ( other.resource.getName().morePreciseThan( resource.getName() ) ) {
				return -1;
			}
			res = Boolean.valueOf( source.isExplicit() ).compareTo( other.source.isExplicit() );
			if ( res != 0 ) {
				return res;
			}
			return Integer.valueOf( nr ).compareTo( other.nr );
		}

	}

}