package de.jbee.inject.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.jbee.inject.Dependency;
import de.jbee.inject.Injector;
import de.jbee.inject.Injectron;
import de.jbee.inject.Precision;
import de.jbee.inject.Type;

/**
 * The default {@link Injector} that gets the initial {@link Injectron}s from a
 * {@link InjectronSource}.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public final class SourcedInjector
		implements Injector {

	public static Injector create( InjectronSource source ) {
		return new SourcedInjector( source );
	}

	private final Map<Class<?>, Injectron<?>[]> injectrons;

	private SourcedInjector( InjectronSource source ) {
		super();
		this.injectrons = initFrom( source );
	}

	private Map<Class<?>, Injectron<?>[]> initFrom( InjectronSource source ) {
		Injectron<?>[] injectrons = source.exportTo( this );
		Arrays.sort( injectrons, Precision.RESOURCE_COMPARATOR );
		Map<Class<?>, Injectron<?>[]> map = new IdentityHashMap<Class<?>, Injectron<?>[]>(
				injectrons.length );
		if ( injectrons.length == 0 ) {
			return map;
		}
		Class<?> lastRawType = injectrons[0].getResource().getType().getRawType();
		int start = 0;
		for ( int i = 0; i < injectrons.length; i++ ) {
			Class<?> rawType = injectrons[i].getResource().getType().getRawType();
			if ( rawType != lastRawType ) {
				map.put( lastRawType, Arrays.copyOfRange( injectrons, start, i ) );
				start = i;
			}
			lastRawType = rawType;
		}
		map.put( lastRawType, Arrays.copyOfRange( injectrons, start, injectrons.length ) );
		return map;
	}

	@SuppressWarnings ( "unchecked" )
	@Override
	public <T> T resolve( Dependency<T> dependency ) {
		final Type<T> type = dependency.getType();
		final boolean array = type.isUnidimensionalArray();
		if ( !array && type.isLowerBound() ) {
			//TODO return best match from wildcard dependencies (not mapped by raw-type since it doesn't help)
			throw new UnsupportedOperationException(
					"Wildcard-dependencies are not supported yet: " + type );
		}
		Injectron<T> injectron = applicableInjectron( dependency );
		if ( injectron != null ) {
			return injectron.instanceFor( dependency );
		}
		if ( array ) {
			return resolveArray( dependency, type.elementType() );
		}
		if ( type.getRawType() == Injectron.class ) {
			Injectron<?> i = applicableInjectron( dependency.onTypeParameter() );
			if ( i != null ) {
				return (T) i;
			}
		}
		if ( type.getRawType() == Injector.class ) {
			return (T) this;
		}
		throw noInjectronFor( dependency );
	}

	private <T> Injectron<T> applicableInjectron( Dependency<T> dependency ) {
		return mostPreciseOf( typeInjectrons( dependency.getType() ), dependency );
	}

	private <T> Injectron<T> mostPreciseOf( Injectron<T>[] injectrons, Dependency<T> dependency ) {
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
		if ( elementType.getRawType() == Injectron.class ) {
			return resolveInjectronArray( dependency, elementType, elementType.getParameters()[0] );
		}
		Injectron<E>[] elementInjectrons = typeInjectrons( elementType );
		if ( elementInjectrons != null ) {
			List<E> elements = new ArrayList<E>( elementInjectrons.length );
			addAllApplicable( elements, dependency, elementType, elementInjectrons );
			return toArray( elements, elementType );
		}
		// if there hasn't been binds to that specific wildcard Type  
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
		// FIXME it is a difference if all available didn't fit or there hasn't been some. just throw exception in the latter case 
		throw new RuntimeException( "No injectron for array type: " + dependency.getType() );
	}

	private <T, E, I> T resolveInjectronArray( Dependency<T> dependency, Type<E> elementType,
			Type<I> injectronType ) {
		Injectron<I>[] res = typeInjectrons( injectronType );
		List<Injectron<I>> elements = new ArrayList<Injectron<I>>( res.length );
		Dependency<I> injectornDependency = dependency.typed( injectronType );
		for ( Injectron<I> i : res ) {
			if ( i.getResource().isAdequateFor( injectornDependency )
					&& i.getResource().isAssignableTo( injectornDependency ) ) {
				elements.add( i );
			}
		}
		return (T) elements.toArray( new Injectron<?>[elements.size()] );
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
	private <T> Injectron<T>[] typeInjectrons( Type<T> type ) {
		return (Injectron<T>[]) injectrons.get( type.getRawType() );
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		for ( Entry<Class<?>, Injectron<?>[]> e : injectrons.entrySet() ) {
			b.append( e.getKey() ).append( '\n' );
			for ( Injectron<?> i : e.getValue() ) {
				b.append( '\t' ).append( i ).append( '\n' );
			}
		}
		return b.toString();
	}
}
