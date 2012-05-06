package de.jbee.inject;

import java.lang.reflect.Array;
import java.util.Arrays;
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
		this.injectrons = splitAndMapByRawType( injectrons( bindings ) );
	}

	private Injectron<?>[] injectrons( Binding<?>[] bindings ) {

		return null;
	}

	private Map<Class<?>, Injectron<?>[]> splitAndMapByRawType( Injectron<?>[] injectrons ) {
		Map<Class<?>, Injectron<?>[]> res = new IdentityHashMap<Class<?>, Injectron<?>[]>(
				injectrons.length );
		final int end = injectrons.length - 1;
		int start = 0;
		for ( int i = 0; i <= end; i++ ) {
			Resource<?> r = injectrons[i].getResource();
			Class<?> rawType = r.getType().getRawType();
			if ( i == end || injectrons[i + 1].getResource().getType().getRawType() != rawType ) {
				res.put( rawType, Arrays.copyOfRange( injectrons, start, i + 1 ) );
			}
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
			throw new RuntimeException( "No injectron for type: " + type );
		}
		for ( int i = 0; i < injectrons.length; i++ ) {
			Injectron<T> injectron = injectrons[i];
			if ( injectron.getResource().isApplicableFor( dependency ) ) {
				return injectron.instanceFor( dependency ); //OPEN I guess we need to add information about the type being injected here 
			}
		}
		if ( type.isUnidimensionalArray() ) {
			return resolveArray( type, type.getElementType() );
		}
		return null;
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

	private static class InternalInjectron<T>
			implements Supplier<T>, Injectron<T> {

		final int serialNumber;
		final Resource<T> resource;
		final Supplier<T> supplier;
		final Source source;
		final Repository repository;
		final DependencyResolver context;

		InternalInjectron( int serialNumber, Resource<T> resource, Supplier<T> supplier,
				Source source, Repository repository, DependencyResolver context ) {
			super();
			this.serialNumber = serialNumber;
			this.resource = resource;
			this.supplier = supplier;
			this.source = source;
			this.repository = repository;
			this.context = context;
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
			Injection<T> i = null;
			return repository.yield( i, Suppliers.asInjectable( this, context ) );
		}

		@Override
		public T supply( Dependency<T> dependency, DependencyResolver context ) {
			return supplier.supply( dependency, context );
		}

	}
}
