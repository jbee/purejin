package de.jbee.inject.bind;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import de.jbee.inject.Expiry;
import de.jbee.inject.InjectionStrategy;
import de.jbee.inject.Instance;
import de.jbee.inject.Precision;
import de.jbee.inject.Repository;
import de.jbee.inject.Resource;
import de.jbee.inject.Scope;
import de.jbee.inject.Source;
import de.jbee.inject.Suppliable;
import de.jbee.inject.Supplier;
import de.jbee.inject.Type;
import de.jbee.inject.util.Scoped;
import de.jbee.inject.util.TypeReflector;

public final class Assemble {

	private Assemble() {
		throw new UnsupportedOperationException( "util" );
	}

	public static final Assembler BUILDIN = new BindingAssembler();
	public static final InjectionStrategy DEFAULE_INJECTION_STRATEGY = new BuildinInjectionStrategy();

	private static class BuildinInjectionStrategy
			implements InjectionStrategy {

		BuildinInjectionStrategy() {
			// make visible
		}

		@Override
		public <T> Constructor<T> constructorFor( Class<T> type ) {
			try {
				return TypeReflector.defaultConstructor( type );
			} catch ( RuntimeException e ) {
				return null;
			}
		}

		@Override
		public <T> Instance<?>[] parametersFor( Constructor<T> constructor ) {
			return Instance.anyOf( Type.parameterTypes( constructor ) );
		}

	}

	private static class BindingAssembler
			implements Assembler {

		BindingAssembler() {
			super();
		}

		@Override
		public Suppliable<?>[] assemble( Module[] modules, InjectionStrategy strategy ) {
			return install( cleanedUp( bindingsFrom( modules, strategy ) ) );
		}

		private Suppliable<?>[] install( Binding<?>[] bindings ) {
			Map<Scope, Repository> repositories = initRepositories( bindings );
			Suppliable<?>[] suppliables = new Suppliable<?>[bindings.length];
			//TODO use real expiry data
			Expiry expiration = Expiry.NEVER;
			for ( int i = 0; i < bindings.length; i++ ) {
				Binding<?> binding = bindings[i];
				Scope scope = binding.scope;
				suppliables[i] = binding.suppliableIn( repositories.get( scope ),
						Expiry.expires( scope == Scoped.INJECTION
							? 1
							: 0 ) );
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
			// TODO snapshot wrappers  
			return repositories;
		}

		private Binding<?>[] cleanedUp( Binding<?>[] bindings ) {
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
					throw new IllegalStateException( "Duplicate binds:" + one + "," + other );
				}
			}
			return res.toArray( new Binding[res.size()] );
		}

		private Binding<?>[] bindingsFrom( Module[] modules, InjectionStrategy strategy ) {
			ListBindings bindings = new ListBindings();
			for ( Module m : modules ) {
				m.declare( bindings, strategy );
			}
			return bindings.list.toArray( new Binding<?>[0] );
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
			int res = Precision.comparePrecision( resource.getInstance(),
					other.resource.getInstance() );
			if ( res != 0 ) {
				return res;
			}
			//TODO what about the Availability ? 
			res = Precision.comparePrecision( source, other.source );
			if ( res != 0 ) {
				return res;
			}
			return -1; // keep order
		}

		@Override
		public String toString() {
			return source + " / " + resource + " / " + scope;
		}

		Suppliable<T> suppliableIn( Repository repository, Expiry expiration ) {
			return new Suppliable<T>( resource, supplier, repository, expiration, source );
		}

	}
}
