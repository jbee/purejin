package de.jbee.inject.util;

import static de.jbee.inject.Demand.demand;
import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Emergence.emergence;

import java.util.Arrays;

import de.jbee.inject.Demand;
import de.jbee.inject.Dependency;
import de.jbee.inject.Expiry;
import de.jbee.inject.Injectable;
import de.jbee.inject.Injector;
import de.jbee.inject.Injectron;
import de.jbee.inject.Precision;
import de.jbee.inject.Repository;
import de.jbee.inject.Resource;
import de.jbee.inject.Source;
import de.jbee.inject.Supplier;

/**
 * A util to create {@link Injectable}s and {@link Injectron}s from {@link Suppliable}s.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public class Injectorizer
		implements InjectronSource {

	public static <T> Injectable<T> asInjectable( Supplier<? extends T> supplier, Injector injector ) {
		return new SupplierToInjectable<T>( supplier, injector );
	}

	public static InjectronSource source( Suppliable<?>[] suppliables ) {
		return new Injectorizer( suppliables );
	}

	private static class SupplierToInjectable<T>
			implements Injectable<T> {

		private final Supplier<? extends T> supplier;
		private final Injector context;

		SupplierToInjectable( Supplier<? extends T> supplier, Injector context ) {
			super();
			this.supplier = supplier;
			this.context = context;
		}

		@Override
		public T instanceFor( Demand<T> demand ) {
			return supplier.supply( demand.getDependency(), context );
		}
	}

	private final Suppliable<?>[] suppliables;

	private Injectorizer( Suppliable<?>[] suppliables ) {
		super();
		this.suppliables = suppliables;
	}

	@Override
	public Injectron<?>[] exportTo( Injector injector ) {
		return injectrons( suppliables, injector );
	}

	public static Injectron<?>[] injectrons( Suppliable<?>[] suppliables, Injector injector ) {
		final int total = suppliables.length;
		if ( total == 0 ) {
			return new Injectron<?>[0];
		}
		Arrays.sort( suppliables, Precision.RESOURCE_COMPARATOR );
		Injectron<?>[] res = new Injectron<?>[total];
		for ( int i = 0; i < total; i++ ) {
			res[i] = injectron( suppliables[i], injector, total, i );
		}
		return res;
	}

	private static <T> Injectron<T> injectron( Suppliable<T> s, Injector injector, int cardinality,
			int serialNumber ) {
		Dependency<T> dependency = dependency( s.resource.getInstance() );
		Demand<T> demand = demand( s.resource, dependency, serialNumber, cardinality );
		Injectable<T> injectable = asInjectable( s.supplier, injector );
		StaticInjectron<T> injectron = new StaticInjectron<T>( s.resource, s.source, demand,
				s.expiry, s.repository, injectable );
		return injectron;
	}

	private static class StaticInjectron<T>
			implements Injectron<T> {

		private final Resource<T> resource;
		private final Source source;
		private final Demand<T> demand;
		private final Repository repository;
		private final Injectable<T> injectable;
		private final Expiry expiry;

		StaticInjectron( Resource<T> resource, Source source, Demand<T> demand, Expiry expiry,
				Repository repository, Injectable<T> injectable ) {
			super();
			this.resource = resource;
			this.source = source;
			this.demand = demand;
			this.expiry = expiry;
			this.repository = repository;
			this.injectable = injectable;
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
		public Expiry getExpiry() {
			return expiry;
		}

		@Override
		public T instanceFor( Dependency<? super T> dependency ) {
			return repository.serve( demand.from( dependency.injectingInto( emergence(
					resource.getInstance(), expiry ) ) ), injectable );
		}

		@Override
		public String toString() {
			return demand.toString() + resource.getTarget().toString();
		}
	}
}
