package de.jbee.silk;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

public class Injector
		implements DependencyContext {

	public static Injector create( Module root ) {
		//TODO setup context
		InjectorBinder binder = new InjectorBinder();
		root.configure( binder );

		return new Injector( binder.makeInjectrons() );
	}

	public static Injector create( Injectron<?>[] injectrons ) {
		return new Injector( injectrons );
	}

	private final int cardinality;
	private final Map<Class<?>, Injectron<?>[]> injectrons;

	private Injector( Injectron<?>[] injectrons ) {
		super();
		this.cardinality = injectrons.length;
		this.injectrons = splitAndMapByRawType( injectrons );
	}

	private static Map<Class<?>, Injectron<?>[]> splitAndMapByRawType( Injectron<?>[] injectrons ) {
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
		dependency = dependency.onInjectronCardinality( cardinality );
		// TODO more information to add to dependency ?
		Type<T> type = dependency.getType();
		Injectron<T>[] injectrons = getInjectrons( type );
		if ( injectrons == null ) {
			if ( type.isUnidimensionalArray() ) {
				resolveArray( type, type.getElementType() );
			}
			throw new RuntimeException( "No injectron for type: " + type );
		}
		for ( int i = 0; i < injectrons.length; i++ ) {
			Injectron<T> injectron = injectrons[i];
			if ( injectron.getResource().isApplicableFor( dependency ) ) {
				return injectron.provide( dependency, this ); //OPEN I guess we need to add information about the type being injected here 
			}
		}
		if ( type.isUnidimensionalArray() ) {
			resolveArray( type, type.getElementType() );
		}
		return null;
	}

	private <T, E> T resolveArray( Type<T> type, Type<E> elementType ) {
		Class<E> rawType = elementType.getRawType();
		Injectron<E>[] elementInjectrons = getInjectrons( elementType );
		if ( elementInjectrons != null ) {
			E[] elements = (E[]) Array.newInstance( rawType, elementInjectrons.length );
			for ( int i = 0; i < elementInjectrons.length; i++ ) {
				Dependency<E> elementDependency = null; //TODO
				elements[i] = elementInjectrons[i].provide( elementDependency, this );
			}
			//FIXME check if the elements are isApplicableFor the type required
			return (T) elements;
		}
		throw new RuntimeException( "No injectron for array type: " + type );
	}

	@SuppressWarnings ( "unchecked" )
	private <T> Injectron<T>[] getInjectrons( Type<T> type ) {
		return (Injectron<T>[]) injectrons.get( type.getRawType() );
	}

}
