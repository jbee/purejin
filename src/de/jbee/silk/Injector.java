package de.jbee.silk;

import java.util.ArrayList;
import java.util.List;

public class Injector {

	public static Injector create( Module root ) {
		//TODO setup context
		List<Binding<?>> bindings = new ArrayList<Binding<?>>();
		Binder binder = new InjectorBinder( bindings );
		root.configure( binder );

		return null;
	}

	static class InjectorBinder
			implements Binder {

		final List<Binding<?>> bindings;

		InjectorBinder( List<Binding<?>> bindings ) {
			super();
			this.bindings = bindings;
		}

		@Override
		public <T> void bind( Reference<T> reference, Supplier<T> supplier, Scope scope,
				Source source ) {
			bindings.add( new Binding<T>( reference, supplier, source, scope ) );
		}

	}

	// two steps :
	// 1. collect Bindings via builder
	// 2. create resources from bindings

	// Class -> [ClassType] -> [(Reference, Supplier, Lifespan, Source)]
	// Scope -> Repository

	// Source:
	// implicit/explicit
	// from which Module etc.

	// Lookup:
	// - find list of tuples Resources for required Class
	// - select Resources by reference
	// - yield by asking the resource

	// use array type as 'all you know of that type and that is accessible' - lists and sets can internally just ask for the array so it gets very simple to add List or Set support

	// The Provider-bind can be done in a Default-Module 

	private static class Binding<T> {

		final Reference<T> reference;
		final Supplier<T> supplier;
		final Source source;
		final Scope scope;

		Binding( Reference<T> reference, Supplier<T> supplier, Source source, Scope scope ) {
			super();
			this.reference = reference;
			this.supplier = supplier;
			this.scope = scope;
			this.source = source;
		}

		Resource<T> makeResourceIn( Repository repository, int nr ) {
			return new Resource<T>( nr, reference, supplier, source, repository );
		}
	}

	private static class Resource<T>
			implements Supplier<T> {

		final int nr;
		final Reference<T> reference;
		final Supplier<T> supplier;
		final Source source;
		final Repository repository;

		Resource( int nr, Reference<T> reference, Supplier<T> supplier, Source source,
				Repository repository ) {
			super();
			this.nr = nr;
			this.reference = reference;
			this.supplier = supplier;
			this.source = source;
			this.repository = repository;
		}

		public T yield( Dependency<T> dependency, DependencyResolver resolver ) {
			//FIXME pass dependency.with(nr) - cardinality will be applied by the calling injector context 
			return repository.yield( dependency, new DependencyResourceResolver<T>( this, resolver ) );
		}

		@Override
		public T supply( Dependency<T> dependency, DependencyResolver resolver ) {
			return supplier.supply( dependency, resolver );
		}

	}

	private static class DependencyResourceResolver<T>
			implements ResourceResolver<T> {

		private final Resource<T> resource;
		private final DependencyResolver resolver;

		DependencyResourceResolver( Resource<T> resource, DependencyResolver resolver ) {
			super();
			this.resource = resource;
			this.resolver = resolver;
		}

		@Override
		public T resolve( Dependency<T> dependency ) {
			return resource.supply( dependency, resolver );
		}
	}
}
