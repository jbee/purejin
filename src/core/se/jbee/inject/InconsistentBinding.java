/*
 *  Copyright (c) 2012-2019, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.lang.annotation.Annotation;

/**
 * If there is a statically resolvable problem with a binding (resource in the
 * context of a container) this exception is thrown during bootstrapping. It is
 * never thrown after the bootstrapping step has finished (a {@link Injector}
 * was created successfully).
 * 
 * @see UnresolvableDependency
 */
public final class InconsistentBinding extends RuntimeException {

	public InconsistentBinding(String msg) {
		super(msg);
	}

	public static InconsistentBinding noSuchAnnotationProperty(
			Class<?> property, Class<? extends Annotation> type) {
		return new InconsistentBinding(type.getSimpleName()
			+ " was expected to have a property of type: "
			+ property.getSimpleName());
	}
}