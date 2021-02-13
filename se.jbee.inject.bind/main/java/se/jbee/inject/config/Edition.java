/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

import se.jbee.inject.Packages;
import se.jbee.lang.Type;

import java.lang.annotation.Annotation;
import java.util.EnumSet;

/**
 * An {@link Edition} decides which features are contained in a specific setup.
 *
 * The particular mechanism how a {@link Edition} decides an the basis of a
 * {@link se.jbee.inject.bind.Bundle} or {@link se.jbee.inject.bind.Module} {@link Class} reference if it is
 * {@link #featured(Class)} is abstract and can be implemented in many ways.
 *
 * Common ways are to utilise type level {@link Annotation}s to indicate which
 * feature a {@link Class} represents and {@link Enum}s to decide which features
 * should be included.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
@FunctionalInterface
public interface Edition {

	/**
	 * Default {@link Edition} that has all features. Or in other words will
	 * install all {@link se.jbee.inject.bind.Bundle}s and {@link se.jbee.inject.bind.Module}s.
	 */
	Edition FULL = bundleOrModule -> true;

	/**
	 * @return true if the given {@link Class} of a module or bundle should be
	 *         included in the context created (should be installed).
	 */
	boolean featured(Class<?> bundleOrModule);

	static Edition includes(Packages included) {
		return bundleOrModule -> included.contains(Type.raw(bundleOrModule));
	}

	@SafeVarargs
	static <F extends Enum<F> & Feature<F>> Edition includes(F... featured) {
		if (featured.length == 0) {
			return bundleOrModule -> false;
		}
		final EnumSet<F> set = EnumSet.of(featured[0], featured);
		return bundleOrModule -> {
			F f = featured[0].featureOf(bundleOrModule);
			return f == null || set.contains(f);
		};
	}
}
