package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.jbee.inject.Dependency;
import de.jbee.inject.DependencyResolver;
import de.jbee.inject.Injectable;
import de.jbee.inject.Injection;
import de.jbee.inject.Injectron;
import de.jbee.inject.Precision;
import de.jbee.inject.Repository;
import de.jbee.inject.Resource;
import de.jbee.inject.Source;
import de.jbee.inject.Suppliers;
import de.jbee.inject.Type;

public class BindableInjector
		implements DependencyResolver {

	public static BindableInjector create( Class<? extends Bundle> root, BundleBinder binder ) {
		return new BindableInjector( binder.install( root ) );
	}

	public static BindableInjector create( Binding<?>[] injectrons ) {
		return new BindableInjector( injectrons );
	}

	private final Map<Class<?>, Injectron<?>[]> injectrons;

	private BindableInjector( Binding<?>[] bindings ) {
		super();
		this.injectrons = injectrons( bindings, this );
	}

	private static Map<Class<?>, Injectron<?>[]> injectrons( Binding<?>[] bindings,
			DependencyResolver resolver ) {
		final int total = bindings.length;
		Map<Class<?>, Injectron<?>[]> res = new IdentityHashMap<Class<?>, Injectron<?>[]>( total );
		if ( total == 0 ) {
			return res;
		}
		Arrays.sort( bindings, new Comparator<Binding<?>>() {

			@Override
			public int compare( Binding<?> one, Binding<?> other ) {
				Resource<?> rOne = one.resource();
				Resource<?> rOther = other.resource();
				Class<?> rawOne = rOne.getType().getRawType();
				Class<?> rawOther = rOther.getType().getRawType();
				if ( rawOne != rawOther ) {
					return rawOne.getCanonicalName().compareTo( rawOther.getCanonicalName() );
				}
				return Precision.comparePrecision( rOne, rOther );
			}
		} );
		final int end = total - 1;
		int start = 0;
		Class<?> lastRawType = bindings[0].resource().getType().getRawType();
		for ( int i = 0; i < total; i++ ) {
			Resource<?> r = bindings[i].resource();
			Class<?> rawType = r.getType().getRawType();
			if ( i == end ) {
				if ( rawType != lastRawType ) {
					res.put( lastRawType, createTypeInjectrons( start, i - 1, bindings, resolver ) );
					res.put( rawType, createTypeInjectrons( end, end, bindings, resolver ) );
				} else {
					res.put( rawType, createTypeInjectrons( start, end, bindings, resolver ) );
				}
			} else if ( rawType != lastRawType ) {
				res.put( lastRawType, createTypeInjectrons( start, i - 1, bindings, resolver ) );
				start = i;
			}
			lastRawType = rawType;
		}
		return res;
	}

	private static <T> Injectron<T>[] createTypeInjectrons( int first, int last,
			Binding<?>[] bindings, DependencyResolver resolver ) {
		final int length = last - first + 1;
		@SuppressWarnings ( "unchecked" )
		Injectron<T>[] res = new Injectron[length];
		for ( int i = 0; i < length; i++ ) {
			@SuppressWarnings ( "unchecked" )
			Binding<T> b = (Binding<T>) bindings[i + first];
			res[i] = new InjectronImpl<T>( b.resource(), b.source(), new Injection<T>(
					dependency( b.resource().getType() ), i + first, bindings.length ),
					b.repository(), Suppliers.asInjectable( b.supplier(), resolver ) );
		}
		return res;
	}

	@Override
	public <T> T resolve( Dependency<T> dependency ) {
		// TODO more information to add to dependency ?
		Type<T> type = dependency.getType();
		if ( !type.isUnidimensionalArray() && type.isLowerBound() ) {
			//TODO return best match from wildcard binds (not mapped by raw-type since it doesn't help)
			throw new UnsupportedOperationException( "Wildcard-binds are not supported yet: "
					+ type );
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
		if ( elementType.isLowerBound() ) { // wildcard bindings:
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
