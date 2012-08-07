package de.jbee.inject;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Precision.comparePrecision;

import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A util to create {@link Injectable}s and {@link Injectron}s from {@link Suppliable}s.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public class Injectorizer {

	public static <T> Injectable<T> asInjectable( Supplier<? extends T> supplier,
			DependencyResolver context ) {
		return new SupplierToInjectable<T>( supplier, context );
	}

	private static class SupplierToInjectable<T>
			implements Injectable<T> {

		private final Supplier<? extends T> supplier;
		private final DependencyResolver context;

		SupplierToInjectable( Supplier<? extends T> supplier, DependencyResolver context ) {
			super();
			this.supplier = supplier;
			this.context = context;
		}

		@Override
		public T instanceFor( Resolving<T> resolving ) {
			return supplier.supply( resolving.dependency(), context );
		}
	}

	public static Map<Class<?>, Injectron<?>[]> injectrons( Suppliable<?>[] suppliables,
			DependencyResolver resolver ) {
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
			Suppliable<?>[] suppliables, DependencyResolver resolver ) {
		final int length = last - first + 1;
		@SuppressWarnings ( "unchecked" )
		Injectron<T>[] res = new Injectron[length];
		for ( int i = 0; i < length; i++ ) {
			@SuppressWarnings ( "unchecked" )
			Suppliable<T> b = (Suppliable<T>) suppliables[i + first];
			res[i] = new InjectronImpl<T>( b.resource, b.source, new Resolving<T>( b.resource,
					dependency( b.resource.getInstance() ), i + first, suppliables.length ),
					b.repository, asInjectable( b.supplier, resolver ) );
		}
		return res;
	}

	private static class InjectronImpl<T>
			implements Injectron<T> {

		final Resource<T> resource;
		final Source source;
		final Resolving<T> resolving;
		final Repository repository;
		final Injectable<T> injectable;

		InjectronImpl( Resource<T> resource, Source source, Resolving<T> resolving,
				Repository repository, Injectable<T> injectable ) {
			super();
			this.resource = resource;
			this.source = source;
			this.resolving = resolving;
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
		public T instanceFor( Dependency<? super T> dependency ) {
			return repository.serve(
					resolving.on( dependency.injectingInto( resource.getInstance() ) ), injectable );
		}

		@Override
		public String toString() {
			String res = resolving.toString();
			return res.substring( 0, res.length() - 2 ).concat( resource.getTarget().toString() );
		}
	}
}
