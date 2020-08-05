/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.lang.annotation.Annotation;

/**
 * If there is a statically resolvable problem with a binding ({@link Locator}
 * in the context of a container) this exception is thrown during bootstrapping.
 * It is never thrown after the bootstrapping step has finished (a
 * {@link Injector} was created successfully).
 *
 * @see UnresolvableDependency
 */
public class InconsistentDeclaration extends RuntimeException {

	public InconsistentDeclaration(Exception cause) {
		super(cause);
	}

	public InconsistentDeclaration(String msg, Exception cause) {
		super(msg, cause);
	}

	public InconsistentDeclaration(String msg) {
		super(msg);
	}

	// Text should answer: What is the problem with the binding or in the binding process?

	public static InconsistentDeclaration notConstructable(Class<?> impl) {
		return new InconsistentDeclaration(
				"Attempt to bind a non-constructable type: " + impl);
	}

	public static InconsistentDeclaration incomprehensibleHint(
			Hint<?> hint) {
		return new InconsistentDeclaration(
				"Attempt to give a parameter hint that does not fit the target: "
					+ hint);
	}

	public static InconsistentDeclaration annotationLacksProperty(
			Class<?> property, Class<? extends Annotation> type) {
		return new InconsistentDeclaration("Attempt to use an annotation "
			+ type.getSimpleName() + " that lacks a expected property of type: "
			+ property.getSimpleName());
	}
}
