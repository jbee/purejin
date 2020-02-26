package se.jbee.inject.bootstrap;

import se.jbee.inject.InconsistentDeclaration;
import se.jbee.inject.Instance;

/**
 * Problems related to {@link Binding} and the bootstrapping process.
 * 
 * @since 19.1
 */
public class InconsistentBinding extends InconsistentDeclaration {

	private InconsistentBinding(String msg) {
		super(msg);
	}

	// Text should answer: What is the problem with the binding or in the binding process?

	public static InconsistentBinding contextAlreadyInitialised() {
		return new InconsistentBinding(
				"Attempt to set binding context after it had been initialised already.");
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

	public static InconsistentBinding illegalCompletion(Binding<?> completing,
			BindingType type) {
		return new InconsistentBinding(
				"Attempt to complete a binding with illegal type " + type + " :"
					+ completing);
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

	public static InconsistentBinding noTypeAnnotation(Class<?> type) {
		int annotations = type.getAnnotations().length;
		return new InconsistentBinding(
				"Exepected an annotation on type but found none "
					+ (annotations == 0 ? "" : "that has a custom definition")
					+ ": " + type.getName());
	}

	public static InconsistentBinding noFunctionalInterface(Class<?> type) {
		return new InconsistentBinding(
				"The type must be annotated with @FunctionalInterface to be usable in this role:"
					+ type.getName());
	}
}
