package de.jbee.silk;

import java.util.ArrayList;
import java.util.List;

public class Injector {

	public static Injector create( Module root ) {
		//TODO setup context
		List<BindingImpl<?>> bindings = new ArrayList<BindingImpl<?>>();
		Binder binder = new InjectorBinder( bindings );
		root.configure( binder );

		return null;
	}

	static class InjectorBinder
			implements Binder {

		final List<BindingImpl<?>> bindings;

		InjectorBinder( List<BindingImpl<?>> bindings ) {
			super();
			this.bindings = bindings;
		}

		@Override
		public <T> void bind( Resource<T> resource, Supplier<T> supplier, Scope scope, Source source ) {
			bindings.add( new BindingImpl<T>( resource, supplier, source, scope ) );
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

	private static class BindingImpl<T> {

		final Resource<T> reference;
		final Supplier<T> supplier;
		final Source source;
		final Scope scope;

		BindingImpl( Resource<T> reference, Supplier<T> supplier, Source source, Scope scope ) {
			super();
			this.reference = reference;
			this.supplier = supplier;
			this.scope = scope;
			this.source = source;
		}

		InjectronImpl<T> makeResourceIn( Repository repository, int nr ) {
			return new InjectronImpl<T>( nr, reference, supplier, source, repository );
		}
	}

	private static class InjectronImpl<T>
			implements Supplier<T>, Injectron<T> {

		final int nr;
		final Resource<T> reference;
		final Supplier<T> supplier;
		final Source source;
		final Repository repository;

		InjectronImpl( int nr, Resource<T> reference, Supplier<T> supplier, Source source,
				Repository repository ) {
			super();
			this.nr = nr;
			this.reference = reference;
			this.supplier = supplier;
			this.source = source;
			this.repository = repository;
		}

		public T yield( Dependency<T> dependency, DependencyContext context ) {
			//FIXME pass dependency.with(nr) - cardinality will be applied by the calling injector context 
			return repository.yield( dependency, new ContextDependencyResolver<T>( this, context ) );
		}

		@Override
		public T supply( Dependency<T> dependency, DependencyContext context ) {
			return supplier.supply( dependency, context );
		}

	}

	private static class ContextDependencyResolver<T>
			implements DependencyResolver<T> {

		private final Supplier<T> resource;
		private final DependencyContext context;

		ContextDependencyResolver( Supplier<T> resource, DependencyContext context ) {
			super();
			this.resource = resource;
			this.context = context;
		}

		@Override
		public T resolve( Dependency<T> dependency ) {
			return resource.supply( dependency, context );
		}
	}
}
