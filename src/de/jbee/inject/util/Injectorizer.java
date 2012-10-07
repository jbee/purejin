package de.jbee.inject.util;

import static de.jbee.inject.Demand.demand;
import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Emergence.emergence;
import static de.jbee.inject.Precision.comparePrecision;

import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;

import de.jbee.inject.Demand;
import de.jbee.inject.Dependency;
import de.jbee.inject.Expiry;
import de.jbee.inject.Injectable;
import de.jbee.inject.Injector;
import de.jbee.inject.Injectron;
import de.jbee.inject.Repository;
import de.jbee.inject.Resource;
import de.jbee.inject.Source;
import de.jbee.inject.Supplier;

/**
 * A util to create {@link Injectable}s and {@link Injectron}s from {@link Suppliable}s.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public class Injectorizer {

	public static <T> Injectable<T> asInjectable( Supplier<? extends T> supplier, Injector context ) {
		return new SupplierToInjectable<T>( supplier, context );
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

	public static Map<Class<?>, Injectron<?>[]> injectrons( Suppliable<?>[] suppliables,
			Injector resolver ) {
		final int total = suppliables.length;
		Map<Class<?>, Injectron<?>[]> res = new IdentityHashMap<Class<?>, Injectron<?>[]>( total );
		if ( total == 0 ) {
			return res;
		}
		Arrays.sort( suppliables, new Comparator<Suppliable<?>>() {

			@Override
			public int compare( Suppliable<?> one, Suppliable<?> other ) {
				Resource<?> rOne = one.resource;
				Resource<?> rOther = other.resource;
				Class<?> rawOne = rOne.getType().getRawType();
				Class<?> rawOther = rOther.getType().getRawType();
				if ( rawOne != rawOther ) {
					return rawOne.getCanonicalName().compareTo( rawOther.getCanonicalName() );
				}
				return comparePrecision( rOne, rOther );
			}
		} );
		final int end = total - 1;
		int start = 0;
		Class<?> lastRawType = suppliables[0].resource.getType().getRawType();
		for ( int i = 0; i < total; i++ ) {
			Resource<?> r = suppliables[i].resource;
			Class<?> rawType = r.getType().getRawType();
			if ( i == end ) {
				if ( rawType != lastRawType ) {
					res.put( lastRawType,
							createTypeInjectrons( start, i - 1, suppliables, resolver ) );
					res.put( rawType, createTypeInjectrons( end, end, suppliables, resolver ) );
				} else {
					res.put( rawType, createTypeInjectrons( start, end, suppliables, resolver ) );
				}
			} else if ( rawType != lastRawType ) {
				res.put( lastRawType, createTypeInjectrons( start, i - 1, suppliables, resolver ) );
				start = i;
			}
			lastRawType = rawType;
		}
		return res;
	}

	private static <T> Injectron<T>[] createTypeInjectrons( int first, int last,
			Suppliable<?>[] suppliables, Injector resolver ) {
		final int length = last - first + 1;
		@SuppressWarnings ( "unchecked" )
		Injectron<T>[] res = new Injectron[length];
		int len = suppliables.length;
		for ( int i = 0; i < length; i++ ) {
			@SuppressWarnings ( "unchecked" )
			Suppliable<T> s = (Suppliable<T>) suppliables[i + first];
			Dependency<T> dependency = dependency( s.resource.getInstance() );
			Demand<T> demand = demand( s.resource, dependency, i + first, len );
			Injectable<T> injectable = asInjectable( s.supplier, resolver );
			res[i] = new ResourceInjectron<T>( s.resource, s.source, demand, s.expiry,
					s.repository, injectable );
		}
		return res;
	}

	private static class ResourceInjectron<T>
			implements Injectron<T> {

		private final Resource<T> resource;
		private final Source source;
		private final Demand<T> demand;
		private final Repository repository;
		private final Injectable<T> injectable;
		private final Expiry expiry;

		ResourceInjectron( Resource<T> resource, Source source, Demand<T> demand, Expiry expiry,
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
