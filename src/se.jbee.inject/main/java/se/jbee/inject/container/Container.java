/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.container;

import static java.lang.System.identityHashCode;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.arrayFilter;
import static se.jbee.inject.Utils.arrayFindFirst;
import static se.jbee.inject.Utils.arrayOf;
import static se.jbee.inject.Utils.orElse;
import static se.jbee.inject.container.Cast.initialiserTypeOf;
import static se.jbee.inject.container.Cast.resourcesTypeFor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import se.jbee.inject.Dependency;
import se.jbee.inject.Env;
import se.jbee.inject.Generator;
import se.jbee.inject.InconsistentDeclaration;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;

/**
 * The default {@link Injector} implementation that is based on
 * {@link Resources} created from {@link ResourceDescriptor}s.
 *
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @see Resources for bootstrapping of the {@link Injector} context
 * @see PostConstruct for instance initialisation
 */
public final class Container implements Injector, Env {

	public static Injector injector(ResourceDescriptor<?>... descriptors) {
		return new Container(descriptors).getDecorated();
	}

	private final Resources resources;
	private final PostConstruct postConstruct;
	private final PostConstructObserver postConstructObserver;
	private final Injector decorated;

	private Container(ResourceDescriptor<?>... descriptors) {
		this.resources = new Resources(this::supplyInContext,
				scope -> resolve(scope, Scope.class), descriptors);
		this.postConstruct = new PostConstruct(
				orElse((t, arr) -> arr,
						() -> resolve(Initialiser.Sorter.class)),
				resolve(resourcesTypeFor(initialiserTypeOf(Type.WILDCARD))));
		this.postConstructObserver = resolvePostConstructObserver();
		this.decorated = postConstruct.postConstruct(this);
		resources.initEager();
	}

	private Injector getDecorated() {
		return decorated == null ? this : decorated;
	}

	private PostConstructObserver resolvePostConstructObserver() {
		return PostConstructObserver.merge(
				resolve(PostConstructObserver[].class));
	}

	@Override
	public <T> T property(Name name, Type<T> property, Package scope) {
		try {
			Dependency<T> global = dependency(instance(name, property));
			if (scope == null)
				return resolve(global);
			return resolve(global.injectingInto(scope));
		} catch (UnresolvableDependency e) {
			throw new InconsistentDeclaration(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T resolve(Dependency<T> dep) {
		final Type<T> type = dep.type();
		final Class<T> rawType = type.rawType;
		if (rawType == Injector.class
			&& (dep.instance.name.isAny() || dep.instance.name.isDefault()))
			return (T) decorated;
		if (rawType == Env.class && dep.instance.name.equalTo(Name.AS))
			return (T) this;
		return resolveFromResource(dep, type, rawType);
	}

	@SuppressWarnings("unchecked")
	private <T> T resolveFromResource(Dependency<T> dep, final Type<T> type,
			final Class<T> rawType) {
		boolean isResourceResolution = rawType == Resource.class
			|| rawType == Generator.class;
		if (isResourceResolution) {
			Resource<?> res = mostQualifiedMatchFor(dep.onTypeParameter());
			if (res != null)
				return (T) res;
		} else {
			Resource<T> match = mostQualifiedMatchFor(dep);
			if (match != null)
				return match.generate(dep);
		}
		if (type.arrayDimensions() == 1)
			return resolveArray(dep, type.baseType());
		if (isResourceResolution)
			return (T) resolveFromUpperBound(dep.onTypeParameter());
		return (T) resolveFromUpperBound(dep).generate(
				(Dependency<Object>) dep);
	}

	/**
	 * There is no direct match for the required type but there might be a
	 * wild-card binding, that is a binding capable of producing all sub-types
	 * of a certain super-type.
	 */
	private <T> Resource<?> resolveFromUpperBound(Dependency<T> dep) {
		Type<T> type = dep.type();
		Resource<?> match = arrayFindFirst(resources.forType(Type.WILDCARD),
				c -> type.isAssignableTo(c.type()));
		if (match != null)
			return match;
		throw noResourceFor(dep);
	}

	private <T> Resource<T> mostQualifiedMatchFor(Dependency<T> dep) {
		return mostQualifiedMatchIn(resources.forType(dep.type()), dep);
	}

	private static <T> Resource<T> mostQualifiedMatchIn(Resource<T>[] rs,
			Dependency<T> dep) {
		return arrayFindFirst(rs, rx -> rx.signature.isMatching(dep));
	}

	private <T> NoResourceForDependency noResourceFor(Dependency<T> dep) {
		Type<T> type = dep.type();
		Type<?> listType = type.rawType == Resource.class
			|| type.rawType == Generator.class ? type.parameter(0) : type;
		return new NoResourceForDependency("", dep,
				resources.forType(listType));
	}

	@SuppressWarnings("unchecked")
	private <T, E> T resolveArray(Dependency<T> dep, Type<E> elemType) {
		final Class<E> rawElemType = elemType.rawType;
		if (rawElemType == Resource.class || rawElemType == Generator.class)
			return (T) resolveResources(dep, elemType.parameter(0));
		if (dep.type().rawType.getComponentType().isPrimitive())
			throw new NoResourceForDependency(
					"Primitive arrays cannot be used to inject all instances of the wrapper type. Use the wrapper array instead.",
					dep);
		Set<Integer> identities = new HashSet<>();
		if (!elemType.isUpperBound()) {
			List<E> elements = new ArrayList<>();
			Resource<E>[] elemResources = resources.forType(elemType);
			if (elemResources != null)
				addAllMatching(elements, identities, dep, elemType,
						elemResources);
			return toArray(elements, elemType);
		}
		List<E> elements = new ArrayList<>();
		for (Entry<Class<?>, Resource<?>[]> e : resources.entrySet())
			if (Type.raw(e.getKey()).isAssignableTo(elemType))
				addAllMatching(elements, identities, dep, elemType,
						(Resource<? extends E>[]) e.getValue());
		return toArray(elements, elemType);
	}

	@SuppressWarnings("unchecked")
	private <T, G> Resource<G>[] resolveResources(Dependency<T> dep,
			Type<G> generatedType) {
		Dependency<G> generatedTypeDep = dep.typed(generatedType);
		if (generatedType.isUpperBound())
			return resolveGenericResources(generatedType, generatedTypeDep);
		Resource<G>[] candidates = resources.forType(generatedType);
		return candidates == null
			? new Resource[0]
			: arrayFilter(candidates,
					c -> c.signature.isCompatibleWith(generatedTypeDep));
	}

	@SuppressWarnings("unchecked")
	private <G> Resource<G>[] resolveGenericResources(Type<G> generatedType,
			Dependency<G> generatedTypeDep) {
		List<Resource<?>> res = new ArrayList<>();
		for (Entry<Class<?>, Resource<?>[]> e : resources.entrySet())
			if (raw(e.getKey()).isAssignableTo(generatedType))
				addCompatibleResources(res, generatedTypeDep,
						(Resource<? extends G>[]) e.getValue());
		return toArray(res, raw(Resource.class));
	}

	private static <G> void addCompatibleResources(List<Resource<?>> res,
			Dependency<G> dep, Resource<? extends G>[] candidates) {
		for (Resource<? extends G> candidate : candidates)
			if (candidate.signature.isCompatibleWith(dep))
				res.add(candidate);
	}

	private static <E, T> void addAllMatching(List<E> elements,
			Set<Integer> identities, Dependency<T> dep, Type<E> elementType,
			Resource<? extends E>[] elementResources) {
		Dependency<E> elemDep = dep.typed(elementType);
		for (int i = 0; i < elementResources.length; i++) {
			Resource<? extends E> elemResource = elementResources[i];
			if (elemResource.signature.isMatching(elemDep)) {
				E instance = elemResource.generate(elemDep);
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

	/**
	 * Can be called by a {@link Generator} to create an instance from a
	 * {@link Supplier} and have {@link Initialiser}s applied for it as well as
	 * notifying {@link PostConstructObserver}s.
	 */
	private <T> T supplyInContext(Dependency<? super T> injected,
			Supplier<? extends T> supplier, Resource<T> resource) {
		Injector context = getDecorated();
		T instance = supplier.supply(injected, context);
		if (instance != null
			&& !resource.permanence.scope.equalTo(Scope.reference)) {
			if (postConstruct != null)
				instance = postConstruct.postConstruct(instance, injected,
						context);
			if (postConstructObserver != null
				&& resource.permanence.isPermanent()) {
				postConstructObserver.afterPostConstruct(resource, instance);
			}
		}
		return instance;
	}

	@Override
	public String toString() {
		return resources.toString();
	}

}
