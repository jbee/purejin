/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.container.Cast.injectionCaseTypeFor;

import se.jbee.inject.Dependency;
import se.jbee.inject.Generator;
import se.jbee.inject.Hint;
import se.jbee.inject.InjectionCase;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.UnresolvableDependency;

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

	private final Hint<?>[] hints;
	private final Generator<?>[] generators;
	private final Object[] preResolvedArgs;
	private final int[] lazyArgIndexes;
	private final int lazyArgCount;

	public InjectionSite(Injector injector, Dependency<?> site,
			Hint<?>[] hints) {
		this.site = site;
		this.hints = hints;
		this.generators = new Generator<?>[hints.length];
		this.preResolvedArgs = new Object[hints.length];
		this.lazyArgIndexes = new int[hints.length];
		this.lazyArgCount = preResolveArgs(injector);
	}

	public Object[] args(Injector injector) throws UnresolvableDependency {
		if (lazyArgCount == 0)
			return preResolvedArgs;
		// in this case we have to copy to become thread-safe!
		Object[] args = preResolvedArgs.clone();
		for (int j = 0; j < lazyArgCount; j++) {
			int i = lazyArgIndexes[j];
			Hint<?> hint = hints[i];
			args[i] = generators[i] == null
				? injector.resolve(site.instanced(hint.relativeRef))
				: yield(generators[i], site.instanced(hint.relativeRef));
		}
		return args;
	}

	private int preResolveArgs(Injector injector) {
		int lazyArgCount = 0;
		for (int i = 0; i < generators.length; i++) {
			Hint<?> hint = hints[i];
			if (hint.type().rawType == Injector.class) {
				preResolvedArgs[i] = injector;
			} else if (hint.type().arrayDimensions() == 1) {
				lazyArgIndexes[lazyArgCount++] = i;
			} else if (hint.absoluteRef != null) {
				preResolvedArgs[i] = injector.resolve(hint.absoluteRef);
			} else if (hint.isConstant()) {
				preResolvedArgs[i] = hint.value;
			} else { // relative ref
				Instance<?> ref = hint.relativeRef;
				Dependency<? extends InjectionCase<?>> caseDep = site.typed(
						injectionCaseTypeFor(ref.type)).named(ref.name);
				InjectionCase<?> icase = injector.resolve(caseDep);
				if (icase.scoping.isStableByDesign()) {
					preResolvedArgs[i] = yield(icase,
							site.instanced(hint.relativeRef));
				} else {
					lazyArgIndexes[lazyArgCount++] = i;
					generators[i] = icase;
				}
			}
		}
		return lazyArgCount;
	}

	@SuppressWarnings("unchecked")
	private static <I> I yield(Generator<I> icase, Dependency<?> dep) {
		return icase.yield((Dependency<? super I>) dep);
	}
}