/*
 *  Copyright (c) 2012-2019, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.container.Typecast.injectionCaseTypeFor;

import se.jbee.inject.Dependency;
import se.jbee.inject.InjectionCase;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.bootstrap.BoundParameter.ParameterType;

/**
 * Similar to a call-site each {@linkplain InjectionSite} represents the
 * resolution of arguments from a specific site or path that is represented by a
 * {@link Dependency}.
 */
public final class InjectionSite {

	public final Dependency<?> site;

	private final BoundParameter<?>[] parameters;
	private final InjectionCase<?>[] cases;
	private final Object[] args;

	private final int[] dynamics;
	private int dynamicsLength = 0;

	public InjectionSite(Dependency<?> site, Injector injector,
			BoundParameter<?>[] parameters) {
		this.site = site;
		this.parameters = parameters;
		this.cases = new InjectionCase<?>[parameters.length];
		this.dynamics = new int[parameters.length];
		this.args = initNonDynamicParameters(injector);
	}

	public Object[] args(Injector injector) throws UnresolvableDependency {
		if (dynamicsLength == 0) {
			return args;
		}
		// in this case we have to copy to become thread-safe!
		Object[] args = this.args.clone();
		for (int j = 0; j < dynamicsLength; j++) {
			int i = dynamics[j];
			BoundParameter<?> p = parameters[i];
			switch (p.type) {
			case INSTANCE:
				args[i] = instance(cases[i],
						site.instanced(parameters[i].instance));
				break;
			default:
			case EXTERNAL:
				args[i] = supply(p, site, injector);
			}
		}
		return args;
	}

	private Object[] initNonDynamicParameters(Injector injector) {
		Object[] args = new Object[parameters.length];
		dynamicsLength = 0;
		for (int i = 0; i < cases.length; i++) {
			args[i] = null;
			BoundParameter<?> p = parameters[i];
			if (p.type == ParameterType.INSTANCE
				&& p.type().arrayDimensions() == 1) {
				// in this case there is no single spec, the injector composes
				// the result array from multiple specs
				p = p.external();
				parameters[i] = p;
			}
			switch (p.type) {
			case INSTANCE:
				Dependency<? extends InjectionCase<?>> caseDep = site.typed(
						injectionCaseTypeFor(p.instance.type)).named(
								p.instance.name);
				InjectionCase<?> icase = injector.resolve(caseDep);
				if (icase.scoping.isStableByDesign()) {
					args[i] = instance(icase, site.instanced(p.instance));
				} else {
					dynamics[dynamicsLength++] = i;
					cases[i] = icase;
				}
				break;
			case CONSTANT:
				args[i] = p.value;
				break;
			default:
			case EXTERNAL:
				dynamics[dynamicsLength++] = i;
			}
		}
		return args;
	}

	private static <T> T supply(BoundParameter<T> p, Dependency<?> dep,
			Injector injector) {
		return p.supplier.supply(dep.instanced(anyOf(p.type())), injector);
	}

	@SuppressWarnings("unchecked")
	private static <I> I instance(InjectionCase<I> icase, Dependency<?> dep) {
		return icase.generator.instanceFor((Dependency<? super I>) dep);
	}
}