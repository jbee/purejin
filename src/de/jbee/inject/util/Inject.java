/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package de.jbee.inject.util;

import static de.jbee.inject.Emergence.emergence;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import de.jbee.inject.Type;
import de.jbee.inject.DIRuntimeException.NoSuchResourceException;

public final class Inject {

	public static Injector from( InjectronSource source ) {
		return new SourcedInjector( source );
	}

	public static <T> Injectable<T> asInjectable( Supplier<? extends T> supplier, Injector injector ) {
		return new SupplierToInjectable<T>( supplier, injector );
	}

	public static <T> Injectron<T> injectorn( Injectable<T> injectable, Resource<T> resource,
			Demand<T> demand, Expiry expiry, Repository repository, Source source ) {
		return new StaticInjectron<T>( resource, source, demand, expiry, repository, injectable );
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

	private Inject() {
		throw new UnsupportedOperationException( "util" );
	}

	/**
	 * The default {@link Injector} that gets the initial {@link Injectron}s from a
	 * {@link InjectronSource}.
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 */
	public static final class SourcedInjector
			implements Injector {

		private final Map<Class<?>, Injectron<?>[]> injectrons;

		SourcedInjector( InjectronSource source ) {
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

		private <T> NoSuchResourceException noInjectronFor( Dependency<T> dependency ) {
			return new NoSuchResourceException( dependency, typeInjectrons( dependency.getType() ) );
		}

		private <T, E> T resolveArray( Dependency<T> dependency, Type<E> elementType ) {
			if ( elementType.getRawType() == Injectron.class ) {
				return resolveInjectronArray( dependency, elementType,
						elementType.getParameters()[0] );
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
						@SuppressWarnings ( "unchecked" )
						Injectron<? extends E>[] value = (Injectron<? extends E>[]) e.getValue();
						addAllApplicable( elements, dependency, elementType, value );
					}
				}
				return toArray( elements, elementType );
			}
			throw noInjectronFor( dependency );
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
			@SuppressWarnings ( "unchecked" )
			T array = (T) elements.toArray( new Injectron<?>[elements.size()] );
			return array;
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
