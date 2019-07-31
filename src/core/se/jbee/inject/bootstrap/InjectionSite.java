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
import se.jbee.inject.UnresolvableDependency;

/**
 * Similar to a call-site each {@linkplain InjectionSite} represents the
 * resolution of arguments from a specific site or path that is represented by a
 * {@link Dependency}. This is used in cases where otherwise a dependency would
 * be resolved over and over again for the same {@link Dependency}. For example
 * when injecting a factory method with arguments.
 */
public final class InjectionSite {

	public final Dependency<?> site;

	private final Hint<?>[] hints;
	private final Generator<?>[] generators;
	private final Object[] preResolvedArgs;

	private final int[] dynamics;
	private int dynamicsLength = 0;

	public InjectionSite(Injector injector, Dependency<?> site,
			Hint<?>[] hints) {
		this.site = site;
		this.hints = hints;
		this.generators = new Generator<?>[hints.length];
		this.dynamics = new int[hints.length];
		this.preResolvedArgs = preResolveArgs(injector);
	}

	public Object[] args(Injector injector) throws UnresolvableDependency {
		if (dynamicsLength == 0)
			return preResolvedArgs;
		// in this case we have to copy to become thread-safe!
		Object[] args = this.preResolvedArgs.clone();
		for (int j = 0; j < dynamicsLength; j++) {
			int i = dynamics[j];
			Hint<?> hint = hints[i];
			args[i] = generators[i] == null
				? injector.resolve(site.instanced(hint.relativeRef))
				: yield(generators[i], site.instanced(hints[i].relativeRef));
		}
		return args;
	}

	private Object[] preResolveArgs(Injector injector) {
		Object[] args = new Object[hints.length];
		dynamicsLength = 0;
		for (int i = 0; i < generators.length; i++) {
			args[i] = null;
			Hint<?> hint = hints[i];
			if (hint.absoluteRef != null) {
				args[i] = injector.resolve(hint.absoluteRef);
			} else if (hint.isDynamic()) {
				dynamics[dynamicsLength++] = i;
			} else if (hint.isConstant()) {
				args[i] = hint.value;
			} else {
				Dependency<? extends InjectionCase<?>> caseDep = site.typed(
						injectionCaseTypeFor(hint.relativeRef.type)).named(
								hint.relativeRef.name);
				InjectionCase<?> icase = injector.resolve(caseDep);
				if (icase.scoping.isStableByDesign()) {
					args[i] = yield(icase, site.instanced(hint.relativeRef));
				} else {
					dynamics[dynamicsLength++] = i;
					generators[i] = icase;
				}
			}
		}
		return args;
	}

	@SuppressWarnings("unchecked")
	private static <I> I yield(Generator<I> icase, Dependency<?> dep) {
		return icase.yield((Dependency<? super I>) dep);
	}
}