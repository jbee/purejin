/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.lang.annotation.Annotation;

import se.jbee.inject.bootstrap.Binding;
import se.jbee.inject.bootstrap.BindingType;

/**
 * If there is a statically resolvable problem with a binding (resource in the
 * context of a container) this exception is thrown during bootstrapping. It is
 * never thrown after the bootstrapping step has finished (a {@link Injector}
 * was created successfully).
 * 
 * @see UnresolvableDependency
 */
public final class InconsistentBinding extends RuntimeException {

	private InconsistentBinding(String msg) {
		super(msg);
	}

	// Text should answer: What is the problem with the binding or in the binding process?

	public static InconsistentBinding contextAlreadyInitialised() {
		return new InconsistentBinding(
				"Attempt to set binding context after it had been initialised already.");
	}

	public static InconsistentBinding notConstructible(Class<?> impl) {
		return new InconsistentBinding(
				"Attempt to bind a non-constructible type: " + impl);
	}

	public static InconsistentBinding addingIncomplete(Binding<?> complete) {
		return new InconsistentBinding(
				"Attempt to add an incomplete binding: " + complete);
	}

	public static InconsistentBinding undefinedMacroType(Binding<?> expanded,
			Class<?> macro) {
		return new InconsistentBinding(
				"Attempt to expand value of type " + macro.getName()
					+ " that is not bound to a macro for binding: " + expanded);
	}

	public static InconsistentBinding incomprehensiveHint(Parameter<?> hint) {
		return new InconsistentBinding(
				"Attempt to give a parameter hint that does not fit the target: "
					+ hint);
	}

	public static InconsistentBinding illegalCompletion(Binding<?> completing,
			BindingType type) {
		return new InconsistentBinding(
				"Attempt to complete a binding with illegal type " + type + " :"
					+ completing);
	}

	public static InconsistentBinding annotationLacksProperty(Class<?> property,
			Class<? extends Annotation> type) {
		return new InconsistentBinding("Attempt to use an annotation "
			+ type.getSimpleName() + " that lacks a expected property of type: "
			+ property.getSimpleName());
	}

	public static InconsistentBinding clash(Binding<?> a, Binding<?> b) {
		return new InconsistentBinding(
				"Detected bindings that clash with each other:\n\t" + a + "\n\t"
					+ b);
	}

	public static InconsistentBinding loop(Binding<?> inconsistent,
			Instance<?> linked, Instance<?> bound) {
		return new InconsistentBinding(
				"Detected a self-referential binding: \n\t" + bound + " => "
					+ linked + "\n\t" + inconsistent);
	}

	public static InconsistentBinding noRootBundle() {
		return new InconsistentBinding(
				"No root bundle has been defined for ServiceLoader service via file META-INF/services/se.jbee.inject.bootstrap.Bundle");
	}
}