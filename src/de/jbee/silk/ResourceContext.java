package de.jbee.silk;

public class ResourceContext { // there is one per call to build a root module

	// two steps :
	// 1. collect Bindings via builder
	// 2. create resources from bindings

	// Class -> [ClassType] -> [(Reference, Supplier, Lifespan, Source)]
	// Scope -> Repository

	// Source:
	// implicit/explicit
	// from which Module etc.

	// Lookup:
	// - find list of tuples (Reference, Source, Scope) for required Class
	// - select tuple by reference
	// - check that Scope of tuple is still Open 
	// - if not: throw away closed Repository and create a new one for the now actual Scope object
	// - yield from Repository by wrapping the source in a provider using the source from fetch step and the repo to resolve its dependencies.

	private static class Binding<T> {

		final Reference<T> reference;
		final Supplier<T> source;
		final Scope scope;

		Binding( Reference<T> reference, Supplier<T> source, Scope scope ) {
			super();
			this.reference = reference;
			this.source = source;
			this.scope = scope;
		}

	}

	private static class Resource<T>
			implements Supplier<T> {

		final int nr;
		final Reference<T> reference;
		final Supplier<T> supplier;
		final Repository repository;

		Resource( int nr, Reference<T> reference, Supplier<T> supplier, Repository repository ) {
			super();
			this.nr = nr;
			this.reference = reference;
			this.supplier = supplier;
			this.repository = repository;
		}

		public T yield( Dependency<T> dependency, DependencyResolver resolver ) {
			//FIXME pass dependency.with(nr, cardinality) 
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
