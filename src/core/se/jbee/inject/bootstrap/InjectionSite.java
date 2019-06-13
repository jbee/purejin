/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.container.Cast.injectionCaseTypeFor;

import se.jbee.inject.Dependency;
import se.jbee.inject.InjectionCase;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.bootstrap.Argument.ParameterResolution;

/**
 * Similar to a call-site each {@linkplain InjectionSite} represents the
 * resolution of arguments from a specific site or path that is represented by a
 * {@link Dependency}.
 */
public final class InjectionSite {

	public final Dependency<?> site;

	private final Argument<?>[] params;
	private final InjectionCase<?>[] cases;
	private final Object[] args;

	private final int[] dynamics;
	private int dynamicsLength = 0;

	public InjectionSite(Injector injector, Dependency<?> site,
			Argument<?>[] args) {
		this.site = site;
		this.params = args;
		this.cases = new InjectionCase<?>[args.length];
		this.dynamics = new int[args.length];
		this.args = initNonDynamicParameters(injector);
	}

	public Object[] args(Injector injector) throws UnresolvableDependency {
		if (dynamicsLength == 0)
			return args;
		// in this case we have to copy to become thread-safe!
		Object[] args = this.args.clone();
		for (int j = 0; j < dynamicsLength; j++) {
			int i = dynamics[j];
			Argument<?> p = params[i];
			switch (p.resolution) {
			case SIMPLE:
				args[i] = instance(cases[i],
						site.instanced(params[i].reference));
				break;
			default:
			case HIERARCHICAL:
				args[i] = supply(p, site, injector);
			}
		}
		return args;
	}

	private Object[] initNonDynamicParameters(Injector injector) {
		Object[] args = new Object[params.length];
		dynamicsLength = 0;
		for (int i = 0; i < cases.length; i++) {
			args[i] = null;
			Argument<?> p = params[i];
			if (p.resolution == ParameterResolution.SIMPLE
				&& p.type().arrayDimensions() == 1) {
				// in this case there is no single spec, the injector composes
				// the result array from multiple specs
				//TODO move this to construction time: there is no InjectionCase that can be cached
				p = p.external();
				params[i] = p;
			}
			switch (p.resolution) {
			case SIMPLE:
				Dependency<? extends InjectionCase<?>> caseDep = site.typed(
						injectionCaseTypeFor(p.reference.type)).named(
								p.reference.name);
				InjectionCase<?> icase = injector.resolve(caseDep);
				if (icase.scoping.isStableByDesign()) {
					args[i] = instance(icase, site.instanced(p.reference));
				} else {
					dynamics[dynamicsLength++] = i;
					cases[i] = icase;
				}
				break;
			case NEVER:
				args[i] = p.constant;
				break;
			default:
			case HIERARCHICAL:
				dynamics[dynamicsLength++] = i;
			}
		}
		return args;
	}

	private static <T> T supply(Argument<T> p, Dependency<?> dep,
			Injector injector) {
		return p.supplier.supply(dep.instanced(anyOf(p.type())), injector);
	}

	@SuppressWarnings("unchecked")
	private static <I> I instance(InjectionCase<I> icase, Dependency<?> dep) {
		return icase.yield((Dependency<? super I>) dep);
	}
}