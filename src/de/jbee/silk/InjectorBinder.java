/**
 * 
 */
package de.jbee.silk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class InjectorBinder
		implements Binder {

	private final List<InternalBinding<?>> bindings = new ArrayList<InternalBinding<?>>();

	@Override
	public <T> void bind( Resource<T> resource, Supplier<T> supplier, Scope scope, Source source ) {
		bindings.add( new InternalBinding<T>( resource, supplier, source, scope ) );
	}

	// From Bindings to Injectrons
	// 0. Create Scope-Repositories
	//   a. sort scopes from most stable to most fragile
	// 	 b. init one repository for each scope
	// 	 c. apply snapshots wrapper to repository instances
	// 1. sort bindings
	// 2. remove duplicates (implicit will be sorted after explicit)
	// 3. detect ambiguous bindings (two explicit bindings that have same type and availability)

	/**
	 * OPEN create a intermediate object whose Injectrons can be visited instead of publish the
	 * array here ? Right now this changes the bindings in place a lot so this object shouldn't
	 * exist any longer after a call.
	 */
	public Injectron<?>[] makeInjectrons() {
		Collections.sort( bindings );

		return toInjectrons();
	}

	private Injectron<?>[] toInjectrons() {
		Injectron<?>[] res = new Injectron<?>[bindings.size()];
		for ( int i = 0; i < res.length; i++ ) {
			res[i] = bindings.get( i ).toInjectron( i, null ); //FIXME get real repository
		}
		return res;
	}

	private static class InternalInjectron<T>
			implements Supplier<T>, Injectron<T> {

		final int serialNumber;
		final Resource<T> resource;
		final Supplier<T> supplier;
		final Source source;
		final Repository repository;

		InternalInjectron( int serialNumber, Resource<T> reference, Supplier<T> supplier,
				Source source, Repository repository ) {
			super();
			this.serialNumber = serialNumber;
			this.resource = reference;
			this.supplier = supplier;
			this.source = source;
			this.repository = repository;
		}

		@Override
		public Resource<T> getResource() {
			return resource;
		}

		@Override
		public Source getSource() {
			return source;
		}

		@Override
		public T provide( Dependency<T> dependency, DependencyContext context ) {
			return repository.yield( dependency.onInjectronSerialNumber( serialNumber ),
					Suppliers.asDependencyResolver( this, context ) );
		}

		@Override
		public T supply( Dependency<T> dependency, DependencyContext context ) {
			return supplier.supply( dependency, context );
		}

	}

	private static class InternalBinding<T>
			implements Binding<T>, Comparable<InternalBinding<T>> {

		final Resource<T> reference;
		final Supplier<T> supplier;
		final Source source;
		final Scope scope;

		InternalBinding( Resource<T> reference, Supplier<T> supplier, Source source, Scope scope ) {
			super();
			this.reference = reference;
			this.supplier = supplier;
			this.scope = scope;
			this.source = source;
		}

		Injectron<T> toInjectron( int serialNumber, Repository repository ) {
			return new InternalInjectron<T>( serialNumber, reference, supplier, source, repository );
		}

		@Override
		public int compareTo( InternalBinding<T> other ) {
			final int res = reference.compareTo( other.reference );
			if ( res != 0 ) {
				return res;
			}
			return Boolean.valueOf( source.isExplicit() ).compareTo( other.source.isExplicit() );
		}
	}
}