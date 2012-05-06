/**
 * 
 */
package de.jbee.inject;

import java.util.ArrayList;
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