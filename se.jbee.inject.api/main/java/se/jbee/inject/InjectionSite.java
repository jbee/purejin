/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static se.jbee.inject.Resource.resourceTypeOf;

/**
 * Similar to a call-site each {@linkplain InjectionSite} represents the
 * resolution of arguments from a specific site or path that is represented by a
 * {@link Dependency}. This is used in cases where otherwise a dependency would
 * be resolved over and over again for the same {@link Dependency}. For example
 * when injecting a factory {@link java.lang.reflect.Method} or
 * {@link java.lang.reflect.Constructor} with arguments.
 */
public final class InjectionSite {

	public final Dependency<?> site;

	private final Hint<?>[] actualParameters;
	private final Generator<?>[] generators;
	private final Object[] preResolvedArgs;
	private final int[] lazyArgIndexes;
	private final int lazyArgCount;

	public InjectionSite(Injector context, Dependency<?> site,
			Hint<?>[] actualParameters) {
		this.site = site;
		this.actualParameters = actualParameters;
		this.generators = new Generator<?>[actualParameters.length];
		this.preResolvedArgs = new Object[actualParameters.length];
		this.lazyArgIndexes = new int[actualParameters.length];
		this.lazyArgCount = preResolveArgs(context);
	}

	public Object[] args(Injector context) throws UnresolvableDependency {
		if (lazyArgCount == 0)
			return preResolvedArgs;
		// in this case we have to copy to become thread-safe!
		Object[] args = preResolvedArgs.clone();
		for (int j = 0; j < lazyArgCount; j++) {
			int i = lazyArgIndexes[j];
			Hint<?> hint = actualParameters[i];
			Dependency<?> argDep = site.onInstance(hint.relativeRef).at(hint.at);
			args[i] = generators[i] == null
				? context.resolve(argDep)
				: generate(generators[i], argDep);
		}
		return args;
	}

	private int preResolveArgs(Injector context) {
		int lazyArgIndex = 0;
		for (int i = 0; i < generators.length; i++) {
			Hint<?> hint = actualParameters[i];
			if (hint.type().rawType == Injector.class) {
				preResolvedArgs[i] = context;
			} else if (hint.isConstant()) {
				preResolvedArgs[i] = hint.value;
			} else if (hint.type().arrayDimensions() == 1) {
				lazyArgIndexes[lazyArgIndex++] = i;
			} else if (hint.absoluteRef != null) {
				preResolvedArgs[i] = context.resolve(hint.absoluteRef.at(hint.at));
			} else { // relative ref
				Instance<?> ref = hint.relativeRef;
				Dependency<? extends Resource<?>> resourceDep = site //
						.typed(resourceTypeOf(ref.type)).named(ref.name).at(hint.at);
				Resource<?> resource = context.resolve(resourceDep);
				if (resource.lifeCycle.isPermanent()) {
					//TODO and not has type variable involved
					preResolvedArgs[i] = generate(resource,
							site.onInstance(hint.relativeRef).at(hint.at));
				} else {
					lazyArgIndexes[lazyArgIndex++] = i;
					generators[i] = resource;
				}
			}
		}
		return lazyArgIndex;
	}

	@SuppressWarnings("unchecked")
	private static <I> I generate(Generator<I> gen, Dependency<?> dep) {
		return gen.generate((Dependency<? super I>) dep);
	}
}
