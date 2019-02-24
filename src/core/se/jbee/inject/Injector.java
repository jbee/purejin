/*
 *  Copyright (c) 2012-2017, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;

/**
 * Knows how to *resolve* an instance for a given {@link Dependency}.
 *
 * The process of resolving might include fabrication of instances.
 *
 * An {@link Injector} is immutable (at least from the outside view). Once
 * created it provides a certain set of supported dependencies that can be
 * resolved. All calls to {@link #resolve(Dependency)} always have the same
 * result for the same {@linkplain Dependency}. The only exception to this are
 * scoping effects (expiring and parallel instances).
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
@FunctionalInterface
public interface Injector {

	<T> T resolve(Dependency<T> dependency) throws UnresolvableDependency;

	/* Utility methods */

	default <T> T resolve(Class<T> type) {
		return resolve(dependency(type));
	}

	default <T> T resolve(Type<T> type) {
		return resolve(dependency(type));
	}

	default <T> T resolve(String name, Class<T> type) {
		return resolve(named(name), type);
	}

	default <T> T resolve(Name name, Class<T> type) {
		return resolve(name, raw(type));
	}

	default <T> T resolve(Name name, Type<T> type) {
		return resolve(instance(name, type));
	}

	default <T> T resolve(Instance<T> inst) {
		return resolve(dependency(inst));
	}
}
