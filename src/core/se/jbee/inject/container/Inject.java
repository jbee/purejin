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
import static se.jbee.inject.Scoping.scopingOf;
import static se.jbee.inject.Instance.compareApplicability;
import static se.jbee.inject.Name.DEFAULT;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.container.Typecast.initialiserTypeOf;
import static se.jbee.inject.container.Typecast.specsTypeOf;

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
import se.jbee.inject.Scoping;
import se.jbee.inject.Injector;
import se.jbee.inject.Generator;
import se.jbee.inject.Specification;
import se.jbee.inject.Name;
import se.jbee.inject.Repository;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;

/**
 * Utility to create/use the core containers {@link Injector} and {@link Generator}.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
@SuppressWarnings("unused")
public final class Inject {

	public static Injector container( Injectee<?>... injectees ) {
		return new InjectorImpl( injectees );
	}

	private Inject() {
		throw new UnsupportedOperationException( "util" );
	}

	/**
	 * The default {@link Injector}.
	 *
	 * For each raw type ({@link Class}) all production rules ({@link Specification}
	 * s) are given ordered from most precise to least precise. The first in
	 * order that matches yields the result instance.
	 */
	private static final class InjectorImpl implements Injector {

		private final Map<Class<?>, Specification<?>[]> specsByType;
		private final Specification<?>[] wildcardSpecs;
		final Specification<Initialiser<?>>[] initialisersSpecs;

		InjectorImpl( Injectee<?>... injectees ) {
			this.specsByType = initFrom( injectees );
			this.wildcardSpecs = wildcardSpecs(specsByType);
			this.initialisersSpecs = initInitialisers();
		}
		
		private Specification<Initialiser<?>>[] initInitialisers() {
			Specification<? extends Initialiser<?>>[] initSpecs = 
					resolve(specsTypeOf(initialiserTypeOf(Type.WILDCARD)));
			if (initSpecs.length == 0) {
				return null;
			}
			Initialiser<Injector>[] injectorInitialisers = 
					resolve(initialiserTypeOf(Injector.class).addArrayDimension());
			if (initSpecs.length == injectorInitialisers.length) {
				for (Initialiser<Injector> init : injectorInitialisers)
					init.init(this);
				return null; // no other dynamic initialisers
			}
			List<Specification<? extends Initialiser<?>>> nonInjectorInitialisers = new ArrayList<>();
			for (Specification<? extends Initialiser<?>> spec : initSpecs) {
				if (!spec.resource.type().equalTo(raw(Initialiser.class).parametized(Injector.class))) {
					nonInjectorInitialisers.add(spec);
				}
			}
			// run initialisers for the injector
			for (Initialiser<Injector> init : injectorInitialisers)
				init.init(this);
			return toArray(nonInjectorInitialisers, raw(Specification.class));
		}

		private <T> Map<Class<?>, Specification<?>[]> initFrom( Injectee<?>... injectees ) {
			Map<Scope, Repository> reps = initRepositories( injectees );
			Specification<?>[] specs = new Specification<?>[injectees.length];
			for (int i = 0; i < injectees.length; i++) {
				@SuppressWarnings("unchecked")
				Injectee<T> injectee = (Injectee<T>) injectees[i];
				Scope scope = injectee.scope();
				Repository rep = reps.get(scope);
				Scoping scoping = scopingOf(scope);
				Generator<T> gen = new InjectorGenerator<>(i, this, rep, injectee, scoping);
				specs[i] = new Specification<>(i, injectee.source(), scoping, injectee.resource(), gen);
			}
			Arrays.sort( specs );
			Map<Class<?>, Specification<?>[]> map = new IdentityHashMap<>( specs.length );
			if ( specs.length == 0 )
				return map;
			Class<?> lastRawType = specs[0].resource.type().rawType;
			int start = 0;
			for ( int i = 0; i < specs.length; i++ ) {
				Class<?> rawType = specs[i].resource.type().rawType;
				if ( rawType != lastRawType ) {
					map.put( lastRawType, copyOfRange( specs, start, i ) );
					start = i;
				}
				lastRawType = rawType;
			}
			map.put( lastRawType, copyOfRange( specs, start, specs.length ) );
			return map;
		}

		private static Specification<?>[] wildcardSpecs(Map<Class<?>, Specification<?>[]> specs) {
			List<Specification<?>> res = new ArrayList<>();
			for (Specification<?>[] typeSpecs : specs.values()) {
				for (Specification<?> spec : typeSpecs) {
					if (spec.resource.type().isUpperBound())
						res.add(spec);
				}
			}
			Collections.sort(res);
			return res.size() == 0 ? null : res.toArray(new Specification[res.size()]);
		}

		private static Map<Scope, Repository> initRepositories( Injectee<?>[] injectees ) {
			Map<Scope, Repository> reps = new IdentityHashMap<>();
			for ( Injectee<?> i : injectees ) {
				Scope scope = i.scope();
				Repository repository = reps.get( scope );
				if ( repository == null )
					reps.put( scope, scope.init(injectees.length) );
			}
			return reps;
		}


		@SuppressWarnings ( "unchecked" )
		@Override
		public <T> T resolve( Dependency<T> dep ) {
			//TODO new feature: resolve by annotation type -> as addon with own API that does its type analysis on basis of specs
			final Type<T> type = dep.type();
			if ( type.rawType == Specification.class ) {
				Specification<?> res = specMatching( dep.onTypeParameter() );
				if ( res != null )
					return (T) res;
			}
			if ( type.rawType == Injector.class )
				return (T) this;
			Specification<T> spec = specMatching( dep );
			if ( spec != null )
				return spec.generator.instanceFor( dep );
			if ( type.arrayDimensions() == 1 )
				return resolveArray( dep, type.baseType() );
			return resolveFromUpperBound(dep);
		}

		/**
		 * There is no direct match for the required type but there might be a wild-card binding,
		 * that is a binding capable of producing all sub-types of a certain super-type.
		 */
		@SuppressWarnings ( "unchecked" )
		private <T> T resolveFromUpperBound(Dependency<T> dep) {
			final Type<T> type = dep.type();
			if ( wildcardSpecs != null ) {
				for (int i = 0; i < wildcardSpecs.length; i++) {
					Specification<?> res = wildcardSpecs[i];
					if (type.isAssignableTo(res.resource.type()))
						return (T) res.generator.instanceFor((Dependency<Object>) dep);
				}
			}
			throw noSpecFor( dep );
		}

		private <T> Specification<T> specMatching( Dependency<T> dep ) {
			return mostApplicableOf( specsForType( dep.type() ), dep );
		}

		private static <T> Specification<T> mostApplicableOf( Specification<T>[] specs, Dependency<T> dep ) {
			if ( specs == null )
				return null;
			for ( int i = 0; i < specs.length; i++ ) {
				Specification<T> spec = specs[i];
				if ( spec.resource.isMatching( dep ) )
					return spec;
			}
			return null;
		}

		private <T> NoResourceForDependency noSpecFor( Dependency<T> dep ) {
			return new NoResourceForDependency( dep, specsForType( dep.type() ), "" );
		}

		private <T, E> T resolveArray( Dependency<T> dep, Type<E> elementType ) {
			if ( elementType.rawType == Specification.class ) {
				return resolveSpecArray( dep, elementType.parameter( 0 ) );
			}
			if ( dep.type().rawType.getComponentType().isPrimitive() ) {
				throw new NoResourceForDependency(dep, null,
						"Primitive arrays cannot be used to inject all instances of the wrapper type. Use the wrapper array instead." );
			}
			Set<Integer> identities = new HashSet<>();
			if (!elementType.isUpperBound()) {
				List<E> elements = new ArrayList<>();
				Specification<E>[] elementSpecs = specsForType( elementType );
				if ( elementSpecs != null )
					addAllMatching( elements, identities, dep, elementType, elementSpecs );
				return toArray( elements, elementType );
			}
			List<E> elements = new ArrayList<>();
			for ( Entry<Class<?>, Specification<?>[]> e : specsByType.entrySet() ) {
				if ( Type.raw( e.getKey() ).isAssignableTo( elementType ) ) {
					@SuppressWarnings ( "unchecked" )
					Specification<? extends E>[] spec = (Specification<? extends E>[]) e.getValue();
					addAllMatching( elements, identities, dep, elementType, spec );
				}
			}
			return toArray( elements, elementType );
		}

		private <T, I> T resolveSpecArray( Dependency<T> dep, Type<I> instanceType ) {
			Dependency<I> instanceDep = dep.typed( instanceType );
			if ( instanceType.isUpperBound() ) {
				List<Specification<?>> res = new ArrayList<>();
				for ( Entry<Class<?>, Specification<?>[]> e : specsByType.entrySet() ) {
					if ( raw( e.getKey() ).isAssignableTo( instanceType ) ) {
						@SuppressWarnings ( "unchecked" )
						Specification<? extends I>[] typeSpecs = (Specification<? extends I>[]) e.getValue();
						for ( Specification<? extends I> spec : typeSpecs ) {
							if ( spec.resource.isCompatibleWith( instanceDep ) ) {
								res.add( spec );
							}
						}
					}
				}
				return toArray( res, raw( Specification.class ) );
			}
			Specification<I>[] res = specsForType(instanceType);
			if (res == null)
				return toArray(emptyList(), raw(Specification.class));
			List<Specification<I>> elements = new ArrayList<>(res.length);
			for (Specification<I> spec : res) {
				if (spec.resource.isCompatibleWith(instanceDep))
					elements.add(spec);
			}
			return toArray(elements, raw(Specification.class));
		}

		private static <E, T> void addAllMatching( List<E> elements, Set<Integer> identities,
				Dependency<T> dep, Type<E> elementType, Specification<? extends E>[] elementSpecs ) {
			Dependency<E> elementDep = dep.typed( elementType );
			for ( int i = 0; i < elementSpecs.length; i++ ) {
				Specification<? extends E> spec = elementSpecs[i];
				if ( spec.resource.isMatching( elementDep ) ) {
					E instance = spec.generator.instanceFor( elementDep );
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
		private <T> Specification<T>[] specsForType( Type<T> type ) {
			return (Specification<T>[]) specsByType.get( type.rawType );
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			for ( Entry<Class<?>, Specification<?>[]> e : specsByType.entrySet() ) {
				toString(b, e.getKey().toString(), e.getValue());
			}
			if (wildcardSpecs != null) {
				toString(b, "? extends *", wildcardSpecs);
			}
			return b.toString();
		}

		private static void toString(StringBuilder b, String group, Specification<?>[] specs) {
			b.append( group ).append( '\n' );
			for ( Specification<?> spec : specs ) {
				Resource<?> r = spec.resource;
				b.append( '\t' ).append( r.type().simpleName() ).append( ' ' ).append(
						r.instance.name ).append( ' ' ).append( r.target ).append(
						' ' ).append( spec.source ).append( '\n' );
			}
		}
	}
	
	private static final class InjectorGenerator<T> implements Generator<T> {

		private final InjectorImpl injector;
		private final int serialID;
		private final Repository repository;
		private final Supplier<? extends T> supplier;
		private final Resource<T> resource;
		private final Scoping scoping;
		
		private Class<?> cachedForType;
		private List<Initialiser<? super T>> cachedInitialisers;

		InjectorGenerator(int serialID, InjectorImpl injector, Repository repository, Injectee<T> injectee,	Scoping scoping) {
			this.serialID = serialID;
			this.injector = injector;
			this.repository = repository;
			this.scoping = scoping;
			this.supplier = injectee.supplier();
			this.resource = injectee.resource();
		}
		
		@Override
		public T instanceFor( Dependency<? super T> dep ) {
			final Dependency<? super T> injected = dep.injectingInto( resource, scoping );
			return repository.serve(serialID, injected, () -> {
				T instance = supplier.supply(injected, injector);
				if (instance != null && injector.initialisersSpecs != null) {
					dynamicInitialisationOf(instance, injected);
				}
				return instance;
			});
		}
		
		private void dynamicInitialisationOf(T instance, Dependency<?> context) {
			Class<?> type = instance.getClass();
			if (type == Class.class || type == Specification.class 
					|| Initialiser.class.isAssignableFrom(type)) {
				return;
			}
			if (type != cachedForType) {
				cachedForType = type;
				for (Specification<? extends Initialiser<?>> i : injector.initialisersSpecs) {
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
		private <I extends Initialiser<?>> Initialiser<? super T> initialiser(Class<?> type, Specification<I> spec, Dependency<?> context) {
			Resource<I> initialiser = spec.resource;
			if (!initialiser.target.isAvailableFor(context))
				return null;
			// this is not 100% generic as instance the type is derived from itself could be generic in a relevant way
			// e.g. if List<String> should be initialised but not List<Integer> we just check for List here and fail later on
			if (raw(type).isAssignableTo(initialiser.type().parameter(0))) {
				return (Initialiser<? super T>) spec.generator.instanceFor(dependency(initialiser.instance));
			}
			return null;
		}
	}
}
