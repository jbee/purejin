/**
 * 
 */
package de.jbee.inject;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

class BuildInModuleBinder
		implements ModuleBinder {

	// Find the initial set of bindings
	// 0. create BindInstruction
	// 2. sort bindings
	// 3. remove duplicates (implicit will be sorted after explicit)
	// 4. detect ambiguous bindings (two explicit bindings that have same type and availability)

	// 1. Create Scope-Repositories
	//   a. sort scopes from most stable to most fragile
	// 	 b. init one repository for each scope
	// 	 c. apply snapshots wrapper to repository instances
	@Override
	public Binding<?>[] bind( Module root ) {
		BindInstruction<?>[] instructions = bindInstructions( root );
		Arrays.sort( instructions );
		// TODO Auto-generated method stub
		return null;
	}

	private BindInstruction<?>[] bindInstructions( Module root ) {
		BindInstructionBinder binder = new BindInstructionBinder();
		root.configure( binder );
		return binder.bindings.toArray( new BindInstruction<?>[0] );
	}

	static class BindInstructionBinder
			implements Binder {

		final List<BindInstruction<?>> bindings = new LinkedList<BindInstruction<?>>();

		@Override
		public <T> void bind( Resource<T> resource, Supplier<? extends T> supplier, Scope scope,
				Source source ) {
			bindings.add( new BindInstruction<T>( bindings.size(), resource, supplier, scope,
					source ) );
		}

	}

	static final class BindInstruction<T>
			implements Comparable<BindInstruction<?>> {

		private final int nr;
		private final Resource<T> resource;
		private final Supplier<? extends T> supplier;
		private final Scope scope;
		private final Source source;

		BindInstruction( int nr, Resource<T> resource, Supplier<? extends T> supplier, Scope scope,
				Source source ) {
			super();
			this.nr = nr;
			this.resource = resource;
			this.supplier = supplier;
			this.scope = scope;
			this.source = source;
		}

		Resource<T> getResource() {
			return resource;
		}

		Supplier<? extends T> getSupplier() {
			return supplier;
		}

		Scope getScope() {
			return scope;
		}

		Source getSource() {
			return source;
		}

		@Override
		public int compareTo( BindInstruction<?> other ) {
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

			// TODO Auto-generated method stub
			return 0;
		}

	}

}