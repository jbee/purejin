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

	private final Hint<?>[] parameters;
	private final Generator<?>[] generators;
	private final Object[] preResolvedArgs;
	private final int[] dynamicArgIndexes;
	private final int dynamicArgCount;

	public InjectionSite(Injector context, Dependency<?> site,
			Hint<?>[] actualParameters) {
		this.site = site;
		this.parameters = actualParameters;
		this.generators = new Generator<?>[actualParameters.length];
		this.preResolvedArgs = new Object[actualParameters.length];
		this.dynamicArgIndexes = new int[actualParameters.length];
		this.dynamicArgCount = preResolveArgs(context);
	}

	/**
	 * @since 8.2
	 */
	public boolean hasDynamicArguments() {
		return dynamicArgCount > 0;
	}

	/**
	 * @since 8.2
	 */
	public boolean hasOnlyDynamicArguments() {
		return dynamicArgCount == parameters.length;
	}

	public Object[] args(Injector context) throws UnresolvableDependency {
		if (!hasDynamicArguments())
			return preResolvedArgs;
		if (hasOnlyDynamicArguments()) {
			Object[] args = new Object[dynamicArgCount];
			for (int i = 0; i < args.length; i++)
				args[i] = resolveParameter(context, parameters[i], generators[i]);
			return args;
		}
		// in this case we have to copy to become thread-safe!
		Object[] args = preResolvedArgs.clone();
		for (int j = 0; j < dynamicArgCount; j++) {
			int i = dynamicArgIndexes[j];
			args[i] = resolveParameter(context, parameters[i],	generators[i]);
		}
		return args;
	}

	private Object resolveParameter(Injector context, Hint<?> hint,
			Generator<?> generator) {
		Dependency<?> argDep = site.onInstance(hint.relativeRef).at(hint.at);
		return generator == null
			? context.resolve(argDep)
			: generate(generator, argDep);
	}

	private int preResolveArgs(Injector context) {
		int lazyArgIndex = 0;
		for (int i = 0; i < generators.length; i++) {
			Hint<?> hint = parameters[i];
			if (hint.type().rawType == Injector.class) {
				preResolvedArgs[i] = context;
			} else if (hint.isConstant()) {
				preResolvedArgs[i] = hint.value;
			} else if (hint.type().arrayDimensions() == 1) {
				dynamicArgIndexes[lazyArgIndex++] = i;
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
					dynamicArgIndexes[lazyArgIndex++] = i;
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
