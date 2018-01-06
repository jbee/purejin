/*
 *  Copyright (c) 2012-2017, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.container;

import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Type.raw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import se.jbee.inject.Array;
import se.jbee.inject.Dependency;
import se.jbee.inject.Expiry;
import se.jbee.inject.Initialiser;
import se.jbee.inject.Injector;
import se.jbee.inject.Injectron;
import se.jbee.inject.InjectronInfo;
import se.jbee.inject.Instance;
import se.jbee.inject.Resource;
import se.jbee.inject.Supplier;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;

/**
 * Utility to create/use the core containers {@link Injector} and {@link Injectron}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Inject {

	public static Injector container( Assembly<?>... assemblies ) {
		return new DefaultInjector( assemblies );
	}
	
	private Inject() {
		throw new UnsupportedOperationException( "util" );
	}

	/**
	 * The default {@link Injector}.
	 * 
	 * For each raw type ({@link Class}) all production rules ({@link Injectron}
	 * s) are given ordered from most precise to least precise. The first in
	 * order that matches yields the result instance.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	private static final class DefaultInjector implements Injector {

		private final Map<Class<?>, Injectron<?>[]> injectrons;
		private final Injectron<?>[] wildcardInjectrons;

		DefaultInjector( Assembly<?>... assemblies ) {
			super();
			this.injectrons = initFrom( assemblies );
			this.wildcardInjectrons = wildcardInjectrons(injectrons);
			initInitialisers();
		}
		
		private void initInitialisers() {
			for (Initialiser i : resolve(dependency(Initialiser[].class))) {
				i.init(this);
			}
		}

		private <T> Map<Class<?>, Injectron<?>[]> initFrom( Assembly<?>... assemblies ) {
			Map<Scope, Repository> repositories = initRepositories( assemblies );
			Injectron<?>[] injectrons = new Injectron<?>[assemblies.length];
			for (int i = 0; i < assemblies.length; i++) {
				@SuppressWarnings("unchecked")
				Assembly<T> assembly = (Assembly<T>) assemblies[i];
				Scope scope = assembly.scope();
				Expiry expiry = EXPIRATION.get( scope );
				if ( expiry == null ) {
					expiry = Expiry.NEVER;
				}
				injectrons[i] = new RepositoryInjectron<>(this, repositories.get( scope ), assembly, expiry, i, assemblies.length);
			}
			Arrays.sort( injectrons, COMPARATOR );
			Map<Class<?>, Injectron<?>[]> map = new IdentityHashMap<>( injectrons.length );
			if ( injectrons.length == 0 ) {
				return map;
			}
			Class<?> lastRawType = injectrons[0].info().resource.type().rawType;
			int start = 0;
			for ( int i = 0; i < injectrons.length; i++ ) {
				Class<?> rawType = injectrons[i].info().resource.type().rawType;
				if ( rawType != lastRawType ) {
					map.put( lastRawType, Arrays.copyOfRange( injectrons, start, i ) );
					start = i;
				}
				lastRawType = rawType;
			}
			map.put( lastRawType, Arrays.copyOfRange( injectrons, start, injectrons.length ) );
			return map;
		}
		
		private static Injectron<?>[] wildcardInjectrons(Map<Class<?>, Injectron<?>[]> injectrons) {
			List<Injectron<?>> res = new ArrayList<>();
			for (Injectron<?>[] is : injectrons.values()) {
				for (Injectron<?> i : is) {
					if (i.info().resource.type().isUpperBound()) {
						res.add(i);
					}
				}
			}
			Collections.sort(res, COMPARATOR);
			return res.size() == 0 ? null : res.toArray(new Injectron[res.size()]);
		}
		
		private static Map<Scope, Repository> initRepositories( Assembly<?>[] assemblies ) {
			Map<Scope, Repository> repositories = new IdentityHashMap<>();
			for ( Assembly<?> a : assemblies ) {
				Scope scope = a.scope();
				Repository repository = repositories.get( scope );
				if ( repository == null ) {
					repositories.put( scope, scope.init() );
				}
			}
			return repositories;
		}
		
		
		@SuppressWarnings ( "unchecked" )
		@Override
		public <T> T resolve( Dependency<T> dependency ) {
			final Type<T> type = dependency.type();
			if ( type.rawType == Injectron.class ) {
				Injectron<?> res = injectronMatching( dependency.onTypeParameter() );
				if ( res != null ) {
					return (T) res;
				}
			}
			if ( type.rawType == Injector.class ) {
				return (T) this;
			}
			Injectron<T> injectron = injectronMatching( dependency );
			if ( injectron != null ) {
				return injectron.instanceFor( dependency );
			}
			if ( type.arrayDimensions() == 1 ) {
				return resolveArray( dependency, type.baseType() );
			}
			return resolveFromUpperBound(dependency);
		}

		/**
		 * There is no direct match for the required type but there might be a wild-card binding,
		 * that is a binding capable of producing all sub-types of a certain super-type. 
		 */
		@SuppressWarnings ( "unchecked" )
		private <T> T resolveFromUpperBound(Dependency<T> dependency) {
			final Type<T> type = dependency.type();
			if ( wildcardInjectrons != null ) {
				for (int i = 0; i < wildcardInjectrons.length; i++) {
					Injectron<?> res = wildcardInjectrons[i];
					if (type.isAssignableTo(res.info().resource.type())) {
						return (T) res.instanceFor((Dependency<Object>) dependency);
					}
				}
			}
			throw noInjectronFor( dependency );
		}

		private <T> Injectron<T> injectronMatching( Dependency<T> dependency ) {
			return mostPreciseOf( injectronsForType( dependency.type() ), dependency );
		}

		private static <T> Injectron<T> mostPreciseOf( Injectron<T>[] injectrons, Dependency<T> dependency ) {
			if ( injectrons == null ) {
				return null;
			}
			for ( int i = 0; i < injectrons.length; i++ ) {
				Injectron<T> injectron = injectrons[i];
				if ( injectron.info().resource.isMatching( dependency ) ) {
					return injectron;
				}
			}
			return null;
		}

		private <T> NoResourceForDependency noInjectronFor( Dependency<T> dependency ) {
			return new NoResourceForDependency( dependency, injectronsForType( dependency.type() ), "" );
		}

		private <T, E> T resolveArray( Dependency<T> dependency, Type<E> elementType ) {
			if ( elementType.rawType == Injectron.class ) {
				return resolveInjectronArray( dependency, elementType.parameter( 0 ) );
			}
			Injectron<E>[] elementInjectrons = injectronsForType( elementType );
			if ( elementInjectrons != null ) {
				List<E> elements = new ArrayList<>( elementInjectrons.length );
				addAllMatching( elements, dependency, elementType, elementInjectrons );
				if ( dependency.type().rawType.getComponentType().isPrimitive() ) {
					throw new NoResourceForDependency(dependency, null,
							"Primitive arrays cannot be used to inject all instances of the wrapper type. Use the wrapper array instead." );
				}
				return toArray( elements, elementType );
			}
			// if there hasn't been binds to that specific wild-card Type  
			if ( elementType.isUpperBound() ) { // wild-card dependency:
				List<E> elements = new ArrayList<>();
				for ( Entry<Class<?>, Injectron<?>[]> e : injectrons.entrySet() ) {
					if ( Type.raw( e.getKey() ).isAssignableTo( elementType ) ) {
						@SuppressWarnings ( "unchecked" )
						Injectron<? extends E>[] value = (Injectron<? extends E>[]) e.getValue();
						addAllMatching( elements, dependency, elementType, value );
					}
				}
				return toArray( elements, elementType );
			}
			@SuppressWarnings("unchecked")
			T empty = (T) Array.newInstance(elementType.rawType, 0);
			return empty;
		}

		private <T, I> T resolveInjectronArray( Dependency<T> dependency, Type<I> instanceType ) {
			Dependency<I> instanceDependency = dependency.typed( instanceType );
			if ( instanceType.isUpperBound() ) {
				List<Injectron<?>> res = new ArrayList<>();
				for ( Entry<Class<?>, Injectron<?>[]> e : injectrons.entrySet() ) {
					if ( raw( e.getKey() ).isAssignableTo( instanceType ) ) {
						@SuppressWarnings ( "unchecked" )
						Injectron<? extends I>[] typeInjectrons = (Injectron<? extends I>[]) e.getValue();
						for ( Injectron<? extends I> i : typeInjectrons ) {
							if ( i.info().resource.isCompatibleWith( instanceDependency ) ) {
								res.add( i );
							}
						}
					}
				}
				return toArray( res, raw( Injectron.class ) );
			}
			Injectron<I>[] res = injectronsForType( instanceType );
			List<Injectron<I>> elements = new ArrayList<>( res.length );
			for ( Injectron<I> i : res ) {
				if ( i.info().resource.isCompatibleWith( instanceDependency ) ) {
					elements.add( i );
				}
			}
			return toArray( elements, raw( Injectron.class ) );
		}

		private static <E, T> void addAllMatching( List<E> elements, Dependency<T> dependency,
				Type<E> elementType, Injectron<? extends E>[] elementInjectrons ) {
			Dependency<E> elementDependency = dependency.typed( elementType );
			for ( int i = 0; i < elementInjectrons.length; i++ ) {
				Injectron<? extends E> injectron = elementInjectrons[i];
				if ( injectron.info().resource.isMatching( elementDependency ) ) {
					elements.add( injectron.instanceFor( elementDependency ) );
				}
			}
		}

		@SuppressWarnings ( "unchecked" )
		private static <T, E> T toArray( List<? extends E> elements, Type<E> elementType ) {
			return (T) Array.of( elements, elementType.rawType );
		}

		@SuppressWarnings ( "unchecked" )
		private <T> Injectron<T>[] injectronsForType( Type<T> type ) {
			return (Injectron<T>[]) injectrons.get( type.rawType );
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			for ( Entry<Class<?>, Injectron<?>[]> e : injectrons.entrySet() ) {
				toString(b, e.getKey().toString(), e.getValue());
			}
			if (wildcardInjectrons != null) {
				toString(b, "? extends *", wildcardInjectrons);
			}
			return b.toString();
		}

		private static void toString(StringBuilder b, String group, Injectron<?>[] values) {
			b.append( group ).append( '\n' );
			for ( Injectron<?> i : values ) {
				Resource<?> r = i.info().resource;
				b.append( '\t' ).append( r.type().simpleName() ).append( ' ' ).append(
						r.instance.name ).append( ' ' ).append( r.target ).append(
						' ' ).append( i.info().source ).append( '\n' );
			}
		}
	}

	private static final class RepositoryInjectron<T> implements Injectron<T> {

		private final Injector injector;
		private final Repository repository;
		private final Supplier<? extends T> supplier;
		private final InjectronInfo<T> info;

		RepositoryInjectron(Injector injector, Repository repository, Assembly<T> assembly, Expiry expiry, int serialID, int count) {
			super();
			this.injector = injector;
			this.repository = repository;
			this.supplier = assembly.supplier();
			this.info = new InjectronInfo<>(assembly.resource(), assembly.source(), expiry, serialID, count);
		}

		@Override
		public InjectronInfo<T> info() {
			return info;
		}

		@Override
		public T instanceFor( Dependency<? super T> dependency ) {
			final Dependency<? super T> injected = dependency.injectingInto( info.resource, info.expiry );
			return repository.serve(injected, info, new DependencyProvider<>(supplier, injected, injector) );
		}

	}

	private static final class DependencyProvider<T> implements Provider<T> {

		private final Supplier<? extends T> supplier;
		private final Dependency<? super T> dependency;
		private final Injector injector;

		DependencyProvider(Supplier<? extends T> supplier, Dependency<? super T> dependency, Injector injector) {
			this.supplier = supplier;
			this.dependency = dependency;
			this.injector = injector;
		}

		@Override
		public T provide() {
			return supplier.supply(dependency, injector);
		}
	}
	
	static final IdentityHashMap<Scope, Expiry> EXPIRATION = defaultExpiration();
	
	private static IdentityHashMap<Scope, Expiry> defaultExpiration() {
		IdentityHashMap<Scope, Expiry> map = new IdentityHashMap<>();
		map.put( Scoped.APPLICATION, Expiry.NEVER );
		map.put( Scoped.INJECTION, Expiry.expires( 1000 ) );
		map.put( Scoped.THREAD, Expiry.expires( 500 ) );
		map.put( Scoped.DEPENDENCY_TYPE, Expiry.NEVER );
		map.put( Scoped.TARGET_INSTANCE, Expiry.NEVER );
		map.put( Scoped.DEPENDENCY, Expiry.NEVER );
		return map;
	}
	
	public static final Comparator<Injectron<?>> COMPARATOR = new InjectronComparator();

	private static final class InjectronComparator implements Comparator<Injectron<?>> {

		InjectronComparator() {
			// make visible
		}

		@Override
		public int compare( Injectron<?> one, Injectron<?> other ) {
			Resource<?> r1 = one.info().resource;
			Resource<?> r2 = other.info().resource;
			Class<?> c1 = r1.type().rawType;
			Class<?> c2 = r2.type().rawType;
			if ( c1 != c2 ) {
				if (c1.isAssignableFrom(c2)) {
					return 1;
				}
				if (c2.isAssignableFrom(c1)) {
					return -1;
				}
				return c1.getCanonicalName().compareTo( c2.getCanonicalName() );
			}
			return Instance.comparePrecision( r1, r2 );
		}
	}	
}
