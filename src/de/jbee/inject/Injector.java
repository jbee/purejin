package de.jbee.inject;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Precision.comparePrecision;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Injector
		implements DependencyResolver {

	public static Injector create( Suppliable<?>[] suppliables ) {
		return new Injector( suppliables );
	}

	private final Map<Class<?>, Injectron<?>[]> injectrons;

	private Injector( Suppliable<?>[] suppliables ) {
		super();
		this.injectrons = injectrons( suppliables, this );
	}

	private static Map<Class<?>, Injectron<?>[]> injectrons( Suppliable<?>[] suppliables,
			DependencyResolver resolver ) {
		final int total = suppliables.length;
		Map<Class<?>, Injectron<?>[]> res = new IdentityHashMap<Class<?>, Injectron<?>[]>( total );
		if ( total == 0 ) {
			return res;
		}
		Arrays.sort( suppliables, new Comparator<Suppliable<?>>() {

			@Override
			public int compare( Suppliable<?> one, Suppliable<?> other ) {
				Resource<?> rOne = one.resource();
				Resource<?> rOther = other.resource();
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
		Class<?> lastRawType = suppliables[0].resource().getType().getRawType();
		for ( int i = 0; i < total; i++ ) {
			Resource<?> r = suppliables[i].resource();
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
			res[i] = new InjectronImpl<T>( b.resource(), b.source(), new Injection<T>(
					dependency( b.resource().getType() ), i + first, suppliables.length ),
					b.repository(), Suppliers.asInjectable( b.supplier(), resolver ) );
		}
		return res;
	}

	@Override
	public <T> T resolve( Dependency<T> dependency ) {
		// TODO more information to add to dependency ?
		Type<T> type = dependency.getType();
		if ( !type.isUnidimensionalArray() && type.isLowerBound() ) {
			//TODO return best match from wildcard dependencies (not mapped by raw-type since it doesn't help)
			throw new UnsupportedOperationException(
					"Wildcard-dependencies are not supported yet: " + type );
		}
		Injectron<T> injectron = resolveInjectron( dependency );
		if ( injectron != null ) {
			return injectron.instanceFor( dependency ); //OPEN I guess we need to add information about the target type being injected here
		}
		if ( type.isUnidimensionalArray() ) {
			return resolveArray( dependency, type.getElementType() );
		}
		//TODO support asking for Injectrons --> allows to "store" pre-resolved references
		if ( type.getRawType() == Injectron.class ) {
			Injectron<?> i = resolveInjectron( dependency.onTypeParameter() );
			if ( i != null ) {
				return (T) i;
			}
		}
		throw noInjectronFor( dependency );
	}

	private <T> Injectron<T> resolveInjectron( Dependency<T> dependency ) {
		return mostPrecise( dependency, getInjectrons( dependency.getType() ) );
	}

	private <T> Injectron<T> mostPrecise( Dependency<T> dependency, Injectron<T>[] injectrons ) {
		if ( injectrons == null ) {
			return null;
		}
		for ( int i = 0; i < injectrons.length; i++ ) {
			Injectron<T> injectron = injectrons[i];
			if ( injectron.getResource().isApplicableFor( dependency ) ) {
				return injectron;
			}
		}
		return null;
	}

	private <T> RuntimeException noInjectronFor( Dependency<T> dependency ) {
		return new RuntimeException( "No injectron for type: " + dependency );
	}

	private <T, E> T resolveArray( Dependency<T> dependency, Type<E> elementType ) {
		Injectron<E>[] elementInjectrons = getInjectrons( elementType );
		if ( elementInjectrons != null ) {
			List<E> elements = new ArrayList<E>( elementInjectrons.length );
			addAllApplicable( elements, dependency, elementType, elementInjectrons );
			return toArray( elements, elementType );
		}
		if ( elementType.isLowerBound() ) { // wildcard dependency:
			List<E> elements = new ArrayList<E>();
			for ( Entry<Class<?>, Injectron<?>[]> e : injectrons.entrySet() ) {
				if ( Type.raw( e.getKey() ).isAssignableTo( elementType ) ) {
					//FIXME some of the injectrons are just bridges and such - no real values - recursion causes errors here
					addAllApplicable( elements, dependency, elementType,
							(Injectron<? extends E>[]) e.getValue() );
				}
			}
			if ( elements.size() > 0 ) {
				return toArray( elements, elementType );
			}
		}
		throw new RuntimeException( "No injectron for array type: " + dependency.getType() );
	}

	private <E, T> void addAllApplicable( List<E> elements, Dependency<T> dependency,
			Type<E> elementType, Injectron<? extends E>[] elementInjectrons ) {
		Dependency<E> elementDependency = dependency.typed( elementType );
		for ( int i = 0; i < elementInjectrons.length; i++ ) {
			Injectron<? extends E> injectron = elementInjectrons[i];
			if ( injectron.getResource().isApplicableFor( elementDependency ) ) {
				elements.add( injectron.instanceFor( elementDependency ) );
			}
		}
	}

	@SuppressWarnings ( "unchecked" )
	private <T, E> T toArray( List<E> elements, Type<E> elementType ) {
		return (T) elements.toArray( (E[]) Array.newInstance( elementType.getRawType(),
				elements.size() ) );
	}

	@SuppressWarnings ( "unchecked" )
	private <T> Injectron<T>[] getInjectrons( Type<T> type ) {
		return (Injectron<T>[]) injectrons.get( type.getRawType() );
	}

	private static class InjectronImpl<T>
			implements Injectron<T> {

		final Resource<T> resource;
		final Source source;
		final Injection<T> injection;
		final Repository repository;
		final Injectable<T> injectable;

		InjectronImpl( Resource<T> resource, Source source, Injection<T> injection,
				Repository repository, Injectable<T> injectable ) {
			super();
			this.resource = resource;
			this.source = source;
			this.injection = injection;
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
			return repository.serve( injection.on( dependency ), injectable );
		}

		@Override
		public String toString() {
			return resource + ":" + injection;
		}
	}

}
