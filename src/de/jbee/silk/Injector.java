package de.jbee.silk;

import java.lang.reflect.Array;
import java.util.Map;

public class Injector
		implements DependencyContext {

	public static Injector create( Module root ) {
		//TODO setup context
		InjectorBinder binder = new InjectorBinder();
		root.configure( binder );

		return new Injector( binder.makeInjectrons() );
	}

	private final Map<Class<?>, Injectrons<?>> injectrons;

	private Injector( Map<Class<?>, Injectrons<?>> injectrons ) {
		super();
		this.injectrons = injectrons;
	}

	@Override
	public <T> T resolve( Dependency<T> dependency ) {
		Type<T> type = dependency.getType();
		Injectrons<T> injectrons = getInjectrons( type );
		if ( injectrons == null ) {
			if ( type.is1DimensionArray() ) {
				resolveArray( type, type.getElementType() );
			}
			throw new RuntimeException( "No injectron for type: " + type );
		}
		// TODO add information to dependency
		for ( int i = 0; i < injectrons.size(); i++ ) {
			Injectron<T> injectron = injectrons.at( i );
			if ( injectron.getResource().isApplicableFor( dependency ) ) {
				return injectron.yield( dependency, this ); //OPEN I guess we need to add information about the type being injected here 
			}
		}
		if ( type.is1DimensionArray() ) {
			resolveArray( type, type.getElementType() );
		}
		return null;
	}

	private <T, E> T resolveArray( Type<T> type, Type<E> elementType ) {
		Class<E> rawType = elementType.getRawType();
		Injectrons<E> elementInjectrons = getInjectrons( elementType );
		if ( elementInjectrons != null ) {
			E[] elements = (E[]) Array.newInstance( rawType, elementInjectrons.size() );
			for ( int i = 0; i < elementInjectrons.size(); i++ ) {
				Dependency<E> elementDependency = null; //TODO
				elements[i] = elementInjectrons.at( i ).yield( elementDependency, this );
			}
			//FIXME check if the elements are isApplicableFor the type required
			return (T) elements;
		}
		throw new RuntimeException( "No injectron for array type: " + type );
	}

	@SuppressWarnings ( "unchecked" )
	private <E> Injectrons<E> getInjectrons( Type<E> type ) {
		return (Injectrons<E>) injectrons.get( type.getRawType() );
	}

}
