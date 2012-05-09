/**
 * 
 */
package de.jbee.inject;


class InjectorBinder
		implements Binder {

	@Override
	public <T> void bind( Resource<T> resource, Supplier<T> supplier, Scope scope, Source source ) {

	}

	// From Bindings to Injectrons
	// 0. Create Scope-Repositories
	//   a. sort scopes from most stable to most fragile
	// 	 b. init one repository for each scope
	// 	 c. apply snapshots wrapper to repository instances
	// 1. sort bindings
	// 2. remove duplicates (implicit will be sorted after explicit)
	// 3. detect ambiguous bindings (two explicit bindings that have same type and availability)

}