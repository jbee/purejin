/*
 *  Copyright (c) 2012-2017, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.container.Typecast.injectronTypeOf;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Injectron;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.bootstrap.BoundParameter.ParameterType;

/**
 * Similar to a call-site each {@linkplain InjectionSite} represents the
 * resolution of arguments from a specific site or path that is represented by a
 * {@link Dependency}.
 *
 * @author jan
 */
public final class InjectionSite {

	public final Dependency<?> site;

	private final BoundParameter<?>[] parameters;
	private final Injectron<?>[] injectrons;
	private final Object[] args;

	private final int[] dynamics;
	private int dynamicsLength = 0;

	public InjectionSite(Dependency<?> site, Injector injector, BoundParameter<?>[] parameters) {
		this.site = site;
		this.parameters = parameters;
		this.injectrons = new Injectron<?>[parameters.length];
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
				args[i] = instance(injectrons[i], site.instanced(parameters[i].instance)); break;
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
		for (int i = 0; i < injectrons.length; i++)  {
			args[i] = null;
			BoundParameter<?> p = parameters[i];
			if (p.type == ParameterType.INSTANCE && p.type().arrayDimensions() == 1) {
				// in this case there is no single injectron, the injector composes the result array from multiple injectrons
				p = p.external();
				parameters[i] = p;
			}
			switch (p.type) {
			case INSTANCE:
				Dependency<? extends Injectron<?>> injDep = site.typed( injectronTypeOf(p.instance.type )).named(p.instance.name);
				Injectron<?> inj = injector.resolve(injDep);
				if (inj.info().expiry.isNever()) {
					args[i] = instance(inj, site.instanced(p.instance));
				}else {
					dynamics[dynamicsLength++] = i;
					injectrons[i] = inj;
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

	private static <T> T supply(BoundParameter<T> p, Dependency<?> dependency, Injector injector) {
		return p.supplier.supply( dependency.instanced( anyOf( p.type() ) ), injector );
	}

	@SuppressWarnings ( "unchecked" )
	private static <I> I instance( Injectron<I> injectron, Dependency<?> dependency ) {
		return injectron.instanceFor( (Dependency<? super I>) dependency );
	}
}