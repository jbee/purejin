/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.container;

import static java.lang.System.identityHashCode;
import static java.util.Arrays.copyOfRange;
import static java.util.Collections.emptyList;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Scoping.scopingOf;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.arrayOf;
import static se.jbee.inject.container.Typecast.initialiserTypeOf;
import static se.jbee.inject.container.Typecast.injectionCasesTypeFor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import se.jbee.inject.Dependency;
import se.jbee.inject.Generator;
import se.jbee.inject.InjectionCase;
import se.jbee.inject.Injector;
import se.jbee.inject.Repository;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.Scoping;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;

/**
 * Implements the {@link Injector} container and its {@link Generator}s.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Inject {

	public static Injector container(Injectee<?>... injectees) {
		return new InjectorImpl(injectees);
	}

	private Inject() {
		throw new UnsupportedOperationException("util");
	}

	/**
	 * The default {@link Injector}.
	 *
	 * For each raw type ({@link Class}) all production rules
	 * ({@link InjectionCase} s) are given ordered from most precise to least
	 * precise. The first in order that matches yields the result instance.
	 */
	private static final class InjectorImpl implements Injector {

		private final Map<Class<?>, InjectionCase<?>[]> casesByType;
		private final InjectionCase<?>[] wildcardCases;
		final InjectionCase<Initialiser<?>>[] initialisersCases;

		InjectorImpl(Injectee<?>... injectees) {
			this.casesByType = initFrom(injectees);
			this.wildcardCases = wildcardSpecs(casesByType);
			this.initialisersCases = initInitialisers();
		}

		private InjectionCase<Initialiser<?>>[] initInitialisers() {
			InjectionCase<? extends Initialiser<?>>[] initCases = resolve(
					injectionCasesTypeFor(initialiserTypeOf(Type.WILDCARD)));
			if (initCases.length == 0) {
				return null;
			}
			Initialiser<Injector>[] injectorInitialisers = resolve(
					initialiserTypeOf(Injector.class).addArrayDimension());
			if (initCases.length == injectorInitialisers.length) {
				for (Initialiser<Injector> init : injectorInitialisers)
					init.init(this, this);
				return null; // no other dynamic initialisers
			}
			List<InjectionCase<? extends Initialiser<?>>> nonInjectorInitialisers = new ArrayList<>();
			for (InjectionCase<? extends Initialiser<?>> icase : initCases) {
				if (!icase.resource.type().equalTo(
						raw(Initialiser.class).parametized(Injector.class))) {
					nonInjectorInitialisers.add(icase);
				}
			}
			// run initialisers for the injector
			for (Initialiser<Injector> init : injectorInitialisers)
				init.init(this, this);
			return toArray(nonInjectorInitialisers, raw(InjectionCase.class));
		}

		private <T> Map<Class<?>, InjectionCase<?>[]> initFrom(
				Injectee<?>... injectees) {
			Map<Scope, Repository> reps = initRepositories(injectees);
			InjectionCase<?>[] cases = new InjectionCase<?>[injectees.length];
			for (int i = 0; i < injectees.length; i++) {
				@SuppressWarnings("unchecked")
				Injectee<T> injectee = (Injectee<T>) injectees[i];
				Scope scope = injectee.scope();
				Repository rep = reps.get(scope);
				Scoping scoping = scopingOf(scope);
				//IDEA allow supplier to implement Generator and if so use that directly?
				Generator<T> gen = new InjectorGenerator<>(i, this, rep,
						injectee, scoping);
				cases[i] = new InjectionCase<>(i, injectee.source(), scoping,
						injectee.resource(), gen);
			}
			Arrays.sort(cases);
			Map<Class<?>, InjectionCase<?>[]> map = new IdentityHashMap<>(
					cases.length);
			if (cases.length == 0)
				return map;
			Class<?> lastRawType = cases[0].resource.type().rawType;
			int start = 0;
			for (int i = 0; i < cases.length; i++) {
				Class<?> rawType = cases[i].resource.type().rawType;
				if (rawType != lastRawType) {
					map.put(lastRawType, copyOfRange(cases, start, i));
					start = i;
				}
				lastRawType = rawType;
			}
			map.put(lastRawType, copyOfRange(cases, start, cases.length));
			return map;
		}

		private static InjectionCase<?>[] wildcardSpecs(
				Map<Class<?>, InjectionCase<?>[]> cases) {
			List<InjectionCase<?>> res = new ArrayList<>();
			for (InjectionCase<?>[] casesForType : cases.values()) {
				for (InjectionCase<?> icase : casesForType) {
					if (icase.resource.type().isUpperBound())
						res.add(icase);
				}
			}
			Collections.sort(res);
			return res.size() == 0
				? null
				: res.toArray(new InjectionCase[res.size()]);
		}

		private static Map<Scope, Repository> initRepositories(
				Injectee<?>[] injectees) {
			Map<Scope, Repository> reps = new IdentityHashMap<>();
			for (Injectee<?> i : injectees) {
				Scope scope = i.scope();
				Repository repository = reps.get(scope);
				if (repository == null)
					reps.put(scope, scope.init(injectees.length));
			}
			return reps;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T resolve(Dependency<T> dep) {
			//TODO new feature: resolve by annotation type -> as addon with own API that does its type analysis on basis of specs
			final Type<T> type = dep.type();
			if (type.rawType == InjectionCase.class) {
				InjectionCase<?> res = injectionCaseMatching(
						dep.onTypeParameter());
				if (res != null)
					return (T) res;
			}
			if (type.rawType == Injector.class)
				return (T) this;
			InjectionCase<T> match = injectionCaseMatching(dep);
			if (match != null)
				return match.generator.yield(dep);
			if (type.arrayDimensions() == 1)
				return resolveArray(dep, type.baseType());
			return resolveFromUpperBound(dep);
		}

		/**
		 * There is no direct match for the required type but there might be a
		 * wild-card binding, that is a binding capable of producing all
		 * sub-types of a certain super-type.
		 */
		@SuppressWarnings("unchecked")
		private <T> T resolveFromUpperBound(Dependency<T> dep) {
			final Type<T> type = dep.type();
			if (wildcardCases != null) {
				for (int i = 0; i < wildcardCases.length; i++) {
					InjectionCase<?> res = wildcardCases[i];
					if (type.isAssignableTo(res.resource.type()))
						return (T) res.generator.yield(
								(Dependency<Object>) dep);
				}
			}
			throw noCaseFor(dep);
		}

		private <T> InjectionCase<T> injectionCaseMatching(Dependency<T> dep) {
			return mostQualifiedMatch(injectionCasesForType(dep.type()), dep);
		}

		private static <T> InjectionCase<T> mostQualifiedMatch(
				InjectionCase<T>[] cases, Dependency<T> dep) {
			if (cases == null)
				return null;
			for (int i = 0; i < cases.length; i++) {
				InjectionCase<T> icase = cases[i];
				if (icase.resource.isMatching(dep))
					return icase;
			}
			return null;
		}

		private <T> NoResourceForDependency noCaseFor(Dependency<T> dep) {
			return new NoResourceForDependency(dep,
					injectionCasesForType(dep.type()), "");
		}

		private <T, E> T resolveArray(Dependency<T> dep, Type<E> elemType) {
			if (elemType.rawType == InjectionCase.class) {
				return resolveCaseArray(dep, elemType.parameter(0));
			}
			if (dep.type().rawType.getComponentType().isPrimitive()) {
				throw new NoResourceForDependency(dep, null,
						"Primitive arrays cannot be used to inject all instances of the wrapper type. Use the wrapper array instead.");
			}
			Set<Integer> identities = new HashSet<>();
			if (!elemType.isUpperBound()) {
				List<E> elements = new ArrayList<>();
				InjectionCase<E>[] elemCases = injectionCasesForType(elemType);
				if (elemCases != null)
					addAllMatching(elements, identities, dep, elemType,
							elemCases);
				return toArray(elements, elemType);
			}
			List<E> elements = new ArrayList<>();
			for (Entry<Class<?>, InjectionCase<?>[]> e : casesByType.entrySet()) {
				if (Type.raw(e.getKey()).isAssignableTo(elemType)) {
					@SuppressWarnings("unchecked")
					InjectionCase<? extends E>[] icase = (InjectionCase<? extends E>[]) e.getValue();
					addAllMatching(elements, identities, dep, elemType, icase);
				}
			}
			return toArray(elements, elemType);
		}

		private <T, I> T resolveCaseArray(Dependency<T> dep,
				Type<I> instanceType) {
			Dependency<I> instanceDep = dep.typed(instanceType);
			if (instanceType.isUpperBound()) {
				List<InjectionCase<?>> res = new ArrayList<>();
				for (Entry<Class<?>, InjectionCase<?>[]> e : casesByType.entrySet()) {
					if (raw(e.getKey()).isAssignableTo(instanceType)) {
						@SuppressWarnings("unchecked")
						InjectionCase<? extends I>[] casesForType = (InjectionCase<? extends I>[]) e.getValue();
						for (InjectionCase<? extends I> icase : casesForType) {
							if (icase.resource.isCompatibleWith(instanceDep)) {
								res.add(icase);
							}
						}
					}
				}
				return toArray(res, raw(InjectionCase.class));
			}
			InjectionCase<I>[] res = injectionCasesForType(instanceType);
			if (res == null)
				return toArray(emptyList(), raw(InjectionCase.class));
			List<InjectionCase<I>> elements = new ArrayList<>(res.length);
			for (InjectionCase<I> s : res) {
				if (s.resource.isCompatibleWith(instanceDep))
					elements.add(s);
			}
			return toArray(elements, raw(InjectionCase.class));
		}

		private static <E, T> void addAllMatching(List<E> elements,
				Set<Integer> identities, Dependency<T> dep, Type<E> elementType,
				InjectionCase<? extends E>[] elemCases) {
			Dependency<E> elemDep = dep.typed(elementType);
			for (int i = 0; i < elemCases.length; i++) {
				InjectionCase<? extends E> elemCase = elemCases[i];
				if (elemCase.resource.isMatching(elemDep)) {
					E instance = elemCase.generator.yield(elemDep);
					if (identities.add(identityHashCode(instance)))
						elements.add(instance);
				}
			}
		}

		@SuppressWarnings("unchecked")
		private static <T, E> T toArray(List<? extends E> elements,
				Type<E> elementType) {
			return (T) arrayOf(elements, elementType.rawType);
		}

		@SuppressWarnings("unchecked")
		private <T> InjectionCase<T>[] injectionCasesForType(Type<T> type) {
			return (InjectionCase<T>[]) casesByType.get(type.rawType);
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			for (Entry<Class<?>, InjectionCase<?>[]> e : casesByType.entrySet()) {
				toString(b, e.getKey().toString(), e.getValue());
			}
			if (wildcardCases != null) {
				toString(b, "? extends *", wildcardCases);
			}
			return b.toString();
		}

		private static void toString(StringBuilder b, String group,
				InjectionCase<?>[] cases) {
			b.append(group).append('\n');
			for (InjectionCase<?> icase : cases) {
				Resource<?> r = icase.resource;
				b.append("\t#").append(icase.serialID).append(' ');
				b.append(r.type().simpleName()).append(' ');
				b.append(r.instance.name).append(' ');
				b.append(r.target).append(' ');
				b.append(icase.scoping).append(' ');
				b.append(icase.source).append('\n');
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

		InjectorGenerator(int serialID, InjectorImpl injector,
				Repository repository, Injectee<T> injectee, Scoping scoping) {
			this.serialID = serialID;
			this.injector = injector;
			this.repository = repository;
			this.scoping = scoping;
			this.supplier = injectee.supplier();
			this.resource = injectee.resource();
		}

		@Override
		public T yield(Dependency<? super T> dep) {
			final Dependency<? super T> injected = dep.injectingInto(resource,
					scoping);
			return repository.serve(serialID, injected, () -> {
				T instance = supplier.supply(injected, injector);
				if (instance != null && injector.initialisersCases != null) {
					dynamicInitialisationOf(instance, injected);
				}
				return instance;
			});
		}

		//TODO maybe add support for annotation - revolve Initialiser bound for Annotation class.
		private void dynamicInitialisationOf(T instance,
				Dependency<?> context) {
			Class<?> type = instance.getClass();
			if (type == Class.class || type == InjectionCase.class
				|| Initialiser.class.isAssignableFrom(type)) {
				return;
			}
			if (type != cachedForType) {
				cachedForType = type;
				for (InjectionCase<? extends Initialiser<?>> icase : injector.initialisersCases) {
					Initialiser<? super T> initialiser = initialiser(type,
							icase, context);
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
					i.init(instance, injector);
			}
		}

		@SuppressWarnings("unchecked")
		private <I extends Initialiser<?>> Initialiser<? super T> initialiser(
				Class<?> type, InjectionCase<I> icase, Dependency<?> context) {
			Resource<I> initialiser = icase.resource;
			if (!initialiser.target.isAvailableFor(context))
				return null;
			// this is not 100% generic as instance the type is derived from itself could be generic in a relevant way
			// e.g. if List<String> should be initialised but not List<Integer> we just check for List here and fail later on
			if (raw(type).isAssignableTo(initialiser.type().parameter(0))) {
				return (Initialiser<? super T>) icase.generator.yield(
						dependency(initialiser.instance));
			}
			return null;
		}
	}
}
