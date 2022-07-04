/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.container;

import se.jbee.inject.*;
import se.jbee.inject.UnresolvableDependency.ResourceResolutionFailed;
import se.jbee.lang.Type;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import static java.lang.System.identityHashCode;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Resource.resourcesTypeOf;
import static se.jbee.lang.Type.raw;
import static se.jbee.lang.Utils.*;

/**
 * The default {@link Injector} implementation that is based on
 * {@link Resources} created from {@link ResourceDescriptor}s.
 *
 * @see Resources for bootstrapping of the {@link Injector} context
 * @see LiftResources for instance initialisation
 */
public final class Container implements Injector, Env {

	public static Injector injector(ResourceDescriptor<?>... descriptors) {
		return new Container(descriptors).getBuiltUp();
	}

	private final Resources resources;
	private final LiftResources liftResources;
	private final Observer observer;
	private final Injector builtUp;

	private Container(ResourceDescriptor<?>... descriptors) {
		this.resources = new Resources(this::supplyInContext,
				scope -> resolve(scope, Scope.class), descriptors);
		this.liftResources = new LiftResources(
				orElse((t, arr) -> arr,
						() -> resolve(Lift.Sequencer.class)),
				resolve(resourcesTypeOf(Lift.liftTypeOf(Type.WILDCARD))));
		this.observer = resolvePostConstructObserver();
		this.builtUp = liftResources.lift(this);
		resources.verifyIn(this);
		resources.initEager();
	}

	private Injector getBuiltUp() {
		return builtUp == null ? this : builtUp;
	}

	private Observer resolvePostConstructObserver() {
		return Observer.merge(
				resolve(Observer[].class));
	}

	@Override
	public <T> T property(Name qualifier, Type<T> property, Class<?> ns) {
		try {
			Dependency<T> global = dependency(instance(qualifier, property));
			if (ns == null)
				return resolve(global);
			return resolve(global.injectingInto(ns));
		} catch (UnresolvableDependency e) {
			throw new InconsistentDeclaration(e);
		}
	}

	@SuppressWarnings({ "unchecked", "ChainOfInstanceofChecks" })
	@Override
	public <T> T resolve(Dependency<T> dep) {
		final Type<T> type = dep.type();
		final Class<T> rawType = type.rawType;
		if (rawType == Injector.class
			&& (dep.instance.name.isAny() || dep.instance.name.isDefault()))
			return (T) builtUp;
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
		return (T) resolveFromUpperBound(dep) //
				.generate((Dependency<Object>) dep);
	}

	/**
	 * There is no direct match for the required type but there might be a
	 * wild-card binding, that is a binding capable of producing all sub-types
	 * of a certain super-type.
	 */
	private <T> Resource<?> resolveFromUpperBound(Dependency<T> dep) {
		Type<T> type = dep.type();
		Resource<?> match = arrayFindFirst(resources.forType(Type.WILDCARD),
				r -> type.isAssignableTo(r.type())
						&& r.signature.instance.name.isCompatibleWith(dep.instance.name));
		if (match != null)
			return match;
		throw noResourceFor(dep);
	}

	private <T> Resource<T> mostQualifiedMatchFor(Dependency<T> dep) {
		if (dep.type().equalTo(Type.WILDCARD) && dep.instance.name.isAny())
			throwAmbiguousDependency(dep);
		return mostQualifiedMatchIn(resources.forType(dep.type()), dep);
	}

	private static <T> Resource<T> mostQualifiedMatchIn(Resource<T>[] rs,
			Dependency<T> dep) {
		return arrayFindFirst(rs, rx -> rx.signature.isUsableFor(dep));
	}

	private <T> ResourceResolutionFailed noResourceFor(Dependency<T> dep) {
		Type<T> type = dep.type();
		Type<?> listType = type.rawType == Resource.class
			|| type.rawType == Generator.class ? type.parameter(0) : type;
		return new ResourceResolutionFailed("No matching resource found.", dep,
				resources.forType(listType));
	}

	private static <T> void throwAmbiguousDependency(Dependency<T> dep) {
		throw new ResourceResolutionFailed(
				"Resolving any instance for any type is considered too ambiguous to be intentional",
				dep);
	}

	@SuppressWarnings("unchecked")
	private <T, E> T resolveArray(Dependency<T> dep, Type<E> elemType) {
		final Class<E> rawElemType = elemType.rawType;
		if (rawElemType == Resource.class || rawElemType == Generator.class)
			return (T) resolveArrayElementResources(dep, elemType.parameter(0));
		if (dep.type().rawType.getComponentType().isPrimitive())
			throw new ResourceResolutionFailed(
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

	@SuppressWarnings({"unchecked"})
	private <R, T> Resource<T>[] resolveArrayElementResources(Dependency<R> resourceDep,
			Type<T> generatedType) {
		Dependency<T> dep = resourceDep.typed(generatedType);
		if (generatedType.isUpperBound())
			return resolveArrayElementResourcesForUpperBoundType(generatedType, dep);
		Resource<T>[] candidates = resources.forType(generatedType);
		return candidates == null
			? new Resource[0]
			: arrayFilter(candidates,
					c -> c.signature.isUsableInstanceWise(dep));
	}

	@SuppressWarnings("unchecked")
	private <T> Resource<T>[] resolveArrayElementResourcesForUpperBoundType(
			Type<T> generatedType, Dependency<T> dep) {
		List<Resource<?>> res = new ArrayList<>();
		for (Entry<Class<?>, Resource<?>[]> e : resources.entrySet())
			if (raw(e.getKey()).isAssignableTo(generatedType))
				addCompatibleResources(res, dep,
						(Resource<? extends T>[]) e.getValue());
		return toArray(res, raw(Resource.class));
	}

	private static <T> void addCompatibleResources(List<Resource<?>> res,
			Dependency<T> dep, Resource<? extends T>[] candidates) {
		for (Resource<? extends T> candidate : candidates)
			if (candidate.signature.isUsableInstanceWise(dep))
				res.add(candidate);
	}

	private static <E, T> void addAllMatching(List<E> elements,
			Set<Integer> identities, Dependency<T> dep, Type<E> elementType,
			Resource<? extends E>[] elementResources) {
		Dependency<E> elemDep = dep.typed(elementType);
		for (Resource<? extends E> elemResource : elementResources) {
			if (elemResource.signature.isUsableFor(elemDep)) {
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
	 * {@link Supplier} and have {@link Lift}s applied for it as well as
	 * notifying {@link Observer}s.
	 */
	private <T> T supplyInContext(Dependency<? super T> injected,
			Supplier<? extends T> supplier, Resource<T> resource) {
		Injector context = getBuiltUp();
		T instance = supplier.supply(injected, context);
		if (instance != null
			&& !resource.lifeCycle.scope.equalTo(Scope.reference)) {
			if (liftResources != null)
				instance = liftResources.lift(instance, injected,
						context);
			if (observer != null
				&& resource.lifeCycle.isPermanent()) {
				observer.afterLift(resource, instance);
			}
		}
		return instance;
	}

	@Override
	public String toString() {
		return resources.toString();
	}

}
