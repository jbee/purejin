/*
 *  Copyright (c) 2012-2017, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.container;

import static java.lang.System.identityHashCode;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;
import static java.util.Collections.emptyList;
import static se.jbee.inject.Array.array;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Dependency.pluginsFor;
import static se.jbee.inject.Instance.compareApplicability;
import static se.jbee.inject.Name.DEFAULT;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.container.Typecast.initialiserTypeOf;
import static se.jbee.inject.container.Typecast.injectronsTypeOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import se.jbee.inject.Dependency;
import se.jbee.inject.Expiry;
import se.jbee.inject.Injector;
import se.jbee.inject.Injectron;
import se.jbee.inject.InjectronInfo;
import se.jbee.inject.Name;
import se.jbee.inject.Resource;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;

/**
 * Utility to create/use the core containers {@link Injector} and {@link Injectron}.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Inject {

	public static Injector container( Injectee<?>... assemblies ) {
		return new InjectorImpl( assemblies );
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
	private static final class InjectorImpl implements Injector {

		private final Map<Class<?>, Injectron<?>[]> injectrons;
		private final Injectron<?>[] wildcardInjectrons;
		final Injectron<Initialiser<?>>[] initialisersInjectrons;

		InjectorImpl( Injectee<?>... injectees ) {
			this.injectrons = initFrom( injectees );
			this.wildcardInjectrons = wildcardInjectrons(injectrons);
			this.initialisersInjectrons = initInitialisers();
		}
		
		private Injectron<Initialiser<?>>[] initInitialisers() {
			Injectron<? extends Initialiser<?>>[] initialisers = 
					resolve(injectronsTypeOf(initialiserTypeOf(Type.WILDCARD)));
			if (initialisers.length == 0) {
				return null;
			}
			Initialiser<Injector>[] injectorInitialisers = 
					resolve(initialiserTypeOf(Injector.class).addArrayDimension());
			if (initialisers.length == injectorInitialisers.length) {
				for (Initialiser<Injector> i : injectorInitialisers)
					i.init(this);
				return null; // no other dynamic initialisers
			}
			List<Injectron<? extends Initialiser<?>>> nonInjectorInitialisers = new ArrayList<>();
			for (Injectron<? extends Initialiser<?>> i : initialisers) {
				if (!i.info().resource.type().equalTo(raw(Initialiser.class).parametized(Injector.class))) {
					nonInjectorInitialisers.add(i);
				}
			}
			// run initialisers for the injector
			for (Initialiser<Injector> i : injectorInitialisers)
				i.init(this);
			return toArray(nonInjectorInitialisers, raw(Injectron.class));
		}

		private <T> Map<Class<?>, Injectron<?>[]> initFrom( Injectee<?>... injectees ) {
			Map<Scope, Repository> repositories = initRepositories( injectees );
			Injectron<?>[] injectrons = new Injectron<?>[injectees.length];
			for (int i = 0; i < injectees.length; i++) {
				@SuppressWarnings("unchecked")
				Injectee<T> injectee = (Injectee<T>) injectees[i];
				Scope scope = injectee.scope();
				Expiry expiry = EXPIRATION.get( scope );
				if ( expiry == null )
					expiry = Expiry.NEVER;
				injectrons[i] = new InjectronImpl<>(this, repositories.get( scope ), injectee, expiry, i, injectees.length);
			}
			Arrays.sort( injectrons, COMPARATOR );
			Map<Class<?>, Injectron<?>[]> map = new IdentityHashMap<>( injectrons.length );
			if ( injectrons.length == 0 )
				return map;
			Class<?> lastRawType = injectrons[0].info().resource.type().rawType;
			int start = 0;
			for ( int i = 0; i < injectrons.length; i++ ) {
				Class<?> rawType = injectrons[i].info().resource.type().rawType;
				if ( rawType != lastRawType ) {
					map.put( lastRawType, copyOfRange( injectrons, start, i ) );
					start = i;
				}
				lastRawType = rawType;
			}
			map.put( lastRawType, copyOfRange( injectrons, start, injectrons.length ) );
			return map;
		}

		private static Injectron<?>[] wildcardInjectrons(Map<Class<?>, Injectron<?>[]> injectrons) {
			List<Injectron<?>> res = new ArrayList<>();
			for (Injectron<?>[] is : injectrons.values()) {
				for (Injectron<?> i : is) {
					if (i.info().resource.type().isUpperBound())
						res.add(i);
				}
			}
			Collections.sort(res, COMPARATOR);
			return res.size() == 0 ? null : res.toArray(new Injectron[res.size()]);
		}

		private static Map<Scope, Repository> initRepositories( Injectee<?>[] assemblies ) {
			Map<Scope, Repository> repositories = new IdentityHashMap<>();
			for ( Injectee<?> a : assemblies ) {
				Scope scope = a.scope();
				Repository repository = repositories.get( scope );
				if ( repository == null )
					repositories.put( scope, scope.init() );
			}
			return repositories;
		}


		@SuppressWarnings ( "unchecked" )
		@Override
		public <T> T resolve( Dependency<T> dependency ) {
			//TODO new feature: resolve by annotation type -> as addon with own API that does its type analysis on basis of Injectrons
			final Type<T> type = dependency.type();
			if ( type.rawType == Injectron.class ) {
				Injectron<?> res = injectronMatching( dependency.onTypeParameter() );
				if ( res != null )
					return (T) res;
			}
			if ( type.rawType == Injector.class )
				return (T) this;
			Injectron<T> injectron = injectronMatching( dependency );
			if ( injectron != null )
				return injectron.instanceFor( dependency );
			if ( type.arrayDimensions() == 1 )
				return resolveArray( dependency, type.baseType() );
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
					if (type.isAssignableTo(res.info().resource.type()))
						return (T) res.instanceFor((Dependency<Object>) dependency);
				}
			}
			throw noInjectronFor( dependency );
		}

		private <T> Injectron<T> injectronMatching( Dependency<T> dependency ) {
			return mostApplicableOf( injectronsForType( dependency.type() ), dependency );
		}

		private static <T> Injectron<T> mostApplicableOf( Injectron<T>[] injectrons, Dependency<T> dependency ) {
			if ( injectrons == null )
				return null;
			for ( int i = 0; i < injectrons.length; i++ ) {
				Injectron<T> injectron = injectrons[i];
				if ( injectron.info().resource.isMatching( dependency ) )
					return injectron;
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
			if ( dependency.type().rawType.getComponentType().isPrimitive() ) {
				throw new NoResourceForDependency(dependency, null,
						"Primitive arrays cannot be used to inject all instances of the wrapper type. Use the wrapper array instead." );
			}
			Set<Integer> identities = new HashSet<>();
			if (!elementType.isUpperBound()) {
				List<E> elements = new ArrayList<>();
				Injectron<E>[] elementInjectrons = injectronsForType( elementType );
				if ( elementInjectrons != null )
					addAllMatching( elements, identities, dependency, elementType, elementInjectrons );
				return toArray( elements, elementType );
			}
			List<E> elements = new ArrayList<>();
			for ( Entry<Class<?>, Injectron<?>[]> e : injectrons.entrySet() ) {
				if ( Type.raw( e.getKey() ).isAssignableTo( elementType ) ) {
					@SuppressWarnings ( "unchecked" )
					Injectron<? extends E>[] value = (Injectron<? extends E>[]) e.getValue();
					addAllMatching( elements, identities, dependency, elementType, value );
				}
			}
			return toArray( elements, elementType );
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
			if (res == null)
				return toArray(emptyList(), raw( Injectron.class ));
			List<Injectron<I>> elements = new ArrayList<>( res.length );
			for ( Injectron<I> i : res ) {
				if ( i.info().resource.isCompatibleWith( instanceDependency ) ) {
					elements.add( i );
				}
			}
			return toArray( elements, raw( Injectron.class ) );
		}

		private static <E, T> void addAllMatching( List<E> elements, Set<Integer> identities,
				Dependency<T> dependency, Type<E> elementType, Injectron<? extends E>[] elementInjectrons ) {
			Dependency<E> elementDependency = dependency.typed( elementType );
			for ( int i = 0; i < elementInjectrons.length; i++ ) {
				Injectron<? extends E> injectron = elementInjectrons[i];
				if ( injectron.info().resource.isMatching( elementDependency ) ) {
					E instance = injectron.instanceFor( elementDependency );
					if (identities.add(identityHashCode(instance)))
						elements.add( instance );
				}
			}
		}

		@SuppressWarnings ( "unchecked" )
		private static <T, E> T toArray( List<? extends E> elements, Type<E> elementType ) {
			return (T) array( elements, elementType.rawType );
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
	
	//TODO maybe change so that there is a wrapper class with the info and the interface so that the interface can only have the instanceFor method and become a functional interface
	private static final class InjectronImpl<T> implements Injectron<T> {

		private final InjectorImpl injector;
		private final Repository repository;
		private final Supplier<? extends T> supplier;
		private final InjectronInfo<T> info;
		
		private Class<?> cachedForType;
		private List<Initialiser<? super T>> cachedInitialisers;

		InjectronImpl(InjectorImpl injector, Repository repository, Injectee<T> injectee, 
				Expiry expiry, int serialID, int count) {
			this.injector = injector;
			this.repository = repository;
			this.supplier = injectee.supplier();
			this.info = new InjectronInfo<>(injectee.resource(), injectee.source(), expiry, serialID, count);
		}

		@Override
		public InjectronInfo<T> info() {
			return info;
		}

		@Override
		public T instanceFor( Dependency<? super T> dependency ) {
			final Dependency<? super T> injected = dependency.injectingInto( info.resource, info.expiry );
			return repository.serve(injected, info, () -> {
				T instance = supplier.supply(injected, injector);
				if (instance != null && injector.initialisersInjectrons != null) {
					dynamicInitialisationOf(instance, injected);
				}
				return instance;
			});
		}
		
		private void dynamicInitialisationOf(T instance, Dependency<?> context) {
			Class<?> type = instance.getClass();
			if (type == Class.class || type == Injectron.class 
					|| Initialiser.class.isAssignableFrom(type)) {
				return;
			}
			if (type != cachedForType) {
				cachedForType = type;
				for (Injectron<? extends Initialiser<?>> i : injector.initialisersInjectrons) {
					Initialiser<? super T> initialiser = initialiser(type, i, context);
					if (initialiser != null) {
						if (cachedInitialisers == null) {
							cachedInitialisers = new ArrayList<>();
						}
						cachedInitialisers.add(initialiser);
					}
				}
			}
			if (cachedInitialisers != null) {
				for (Initialiser<? super T> i : cachedInitialisers)
					i.init(instance);
			}
		}
		
		@SuppressWarnings("unchecked")
		private <I extends Initialiser<?>> Initialiser<? super T> initialiser(Class<?> type, Injectron<I> injectron, Dependency<?> context) {
			Resource<I> initialiser = injectron.info().resource;
			if (!initialiser.target.isAvailableFor(context))
				return null;
			// this is not 100% generic as instance the type is derived from itself could be generic in a relevant way
			// e.g. if List<String> should be initialised but not List<Integer> we just check for List here and fail later on
			if (raw(type).isAssignableTo(initialiser.type().parameter(0))) {
				return (Initialiser<? super T>) injectron.instanceFor(dependency(initialiser.instance));
			}
			return null;
		}

		@Override
		public String toString() {
			return info.toString();
		}
	}

	static final IdentityHashMap<Scope, Expiry> EXPIRATION = defaultExpiration();

	private static IdentityHashMap<Scope, Expiry> defaultExpiration() {
		//TODO change this so each expiry states if it has a consistent relation to another one and if so if it expires within the other
		// maybe this can also be a simple method: boolean expiresBefore(Expiry other);
		// users should be able to create on expirys and define which nestings are fine (whitelist principle) - this way unknown expires are reported as problem. but user has to be able to describe a new expiry's relation to existing ones so nesting becomes no problem if it should not be
		IdentityHashMap<Scope, Expiry> map = new IdentityHashMap<>();
		map.put( Scoped.APPLICATION, Expiry.NEVER );
		map.put( Scoped.INJECTION, Expiry.expires( 1000 ) );
		map.put( Scoped.THREAD, Expiry.expires( 500 ) );
		map.put( Scoped.DEPENDENCY_TYPE, Expiry.NEVER );
		map.put( Scoped.TARGET_INSTANCE, Expiry.NEVER );
		map.put( Scoped.DEPENDENCY, Expiry.NEVER );
		return map;
	}

	public static final Comparator<Injectron<?>> COMPARATOR = (one, other) -> {
		Resource<?> r1 = one.info().resource;
		Resource<?> r2 = other.info().resource;
		Class<?> c1 = r1.type().rawType;
		Class<?> c2 = r2.type().rawType;
		if ( c1 != c2 ) {
			if (c1.isAssignableFrom(c2))
				return 1;
			if (c2.isAssignableFrom(c1))
				return -1;
			return c1.getCanonicalName().compareTo( c2.getCanonicalName() );
		}
		return compareApplicability( r1, r2 );
	};
}
