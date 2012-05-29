package de.jbee.inject;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;

public class Injector
		implements DependencyResolver {

	public static Injector create( Module root, ModuleBinder binder ) {
		return new Injector( binder.bind( root ) );
	}

	public static Injector create( Binding<?>[] injectrons ) {
		return new Injector( injectrons );
	}

	private final Map<Class<?>, Injectron<?>[]> injectrons;

	private Injector( Binding<?>[] bindings ) {
		super();
		this.injectrons = injectrons( bindings, this );
	}

	private static Map<Class<?>, Injectron<?>[]> injectrons( Binding<?>[] bindings,
			DependencyResolver resolver ) {
		Arrays.sort( bindings, new Comparator<Binding<?>>() {

			@Override
			public int compare( Binding<?> one, Binding<?> other ) {
				return one.getResource().compareTo( other.getResource() );
			}
		} );
		final int total = bindings.length;
		Map<Class<?>, Injectron<?>[]> res = new IdentityHashMap<Class<?>, Injectron<?>[]>( total );
		final int end = total - 1;
		int start = 0;
		Class<?> lastRawType = bindings[0].getResource().getType().getRawType();
		for ( int i = 0; i < total; i++ ) {
			Resource<?> r = bindings[i].getResource();
			Class<?> rawType = r.getType().getRawType();
			if ( i == end ) {
				if ( rawType != lastRawType ) {
					res.put( rawType, createTypeInjectrons( start, i - 1, bindings, resolver ) );
					res.put( rawType, createTypeInjectrons( end, end, bindings, resolver ) );
				} else {
					res.put( rawType, createTypeInjectrons( start, end, bindings, resolver ) );
				}
			} else if ( rawType != lastRawType ) {
				res.put( rawType, createTypeInjectrons( start, i - 1, bindings, resolver ) );
				start = i;
			}
		}
		return res;
	}

	private static <T> Injectron<T>[] createTypeInjectrons( int first, int last,
			Binding<?>[] bindings, DependencyResolver resolver ) {
		final int length = last - first + 1;
		Injectron<T>[] res = new Injectron[length];
		for ( int i = 0; i < length; i++ ) {
			Binding<T> b = (Binding<T>) bindings[i + first];
			res[i] = new InjectronImpl<T>( b.getResource(), b.getSource(),
					new Injection<T>( Dependency.dependency( b.getResource().getType() ),
							i + first, bindings.length ), b.getRepository(),
					Suppliers.asInjectable( b.getSupplier(), resolver ) );
		}
		return res;
	}

	@Override
	public <T> T resolve( Dependency<T> dependency ) {
		// TODO more information to add to dependency ?
		Type<T> type = dependency.getType();
		Injectron<T>[] injectrons = getInjectrons( type );
		if ( injectrons == null ) {
			if ( type.isUnidimensionalArray() ) {
				return resolveArray( type, type.getElementType() );
			}
			throw noInjectronFor( type );
		}
		for ( int i = 0; i < injectrons.length; i++ ) {
			Injectron<T> injectron = injectrons[i];
			if ( injectron.getResource().isApplicableFor( dependency ) ) {
				return injectron.instanceFor( dependency ); //OPEN I guess we need to add information about the target type being injected here 
			}
		}
		// TODO try wildcard injectrons 
		if ( type.isUnidimensionalArray() ) {
			return resolveArray( type, type.getElementType() );
		}
		throw noInjectronFor( type );
	}

	private <T> RuntimeException noInjectronFor( Type<T> type ) {
		return new RuntimeException( "No injectron for type: " + type );
	}

	private <T, E> T resolveArray( Type<T> type, Type<E> elementType ) {
		Injectron<E>[] elementInjectrons = getInjectrons( elementType );
		if ( elementInjectrons != null ) {
			int length = elementInjectrons.length;
			E[] elements = newArray( elementType, length );
			for ( int i = 0; i < length; i++ ) {
				Dependency<E> elementDependency = null; //TODO
				elements[i] = elementInjectrons[i].instanceFor( elementDependency );
			}
			//FIXME check if the elements are isApplicableFor the type required
			return (T) elements;
		}
		throw new RuntimeException( "No injectron for array type: " + type );
	}

	@SuppressWarnings ( "unchecked" )
	private <E> E[] newArray( Type<E> elementType, int length ) {
		return (E[]) Array.newInstance( elementType.getRawType(), length );
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
		public T instanceFor( Dependency<T> dependency ) {
			return repository.yield( injection.on( dependency ), injectable );
		}

	}

}
