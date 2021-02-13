package se.jbee.inject.bind;

import se.jbee.inject.InconsistentDeclaration;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.lang.Type;

/**
 * Problems related to {@link Binding} and the bootstrapping process.
 *
 * @since 8.1
 */
public final class InconsistentBinding extends InconsistentDeclaration {

	private InconsistentBinding(String msg) {
		super(msg);
	}

	// Text should answer: What is the problem with the binding or in the binding process?

	public static InconsistentBinding generic(String msg) {
		return new InconsistentBinding(msg);
	}

	public static InconsistentBinding contextAlreadyInitialised() {
		return new InconsistentBinding(
				"Attempt to set binding context after it had been initialised already.");
	}

	public static InconsistentBinding addingIncomplete(Binding<?> complete) {
		return new InconsistentBinding(
				"Attempt to add an incomplete binding: " + complete);
	}

	public static InconsistentBinding undefinedValueBinderType(Binding<?> expanded,
			Class<?> macro) {
		return new InconsistentBinding(
				"Attempt to expand value of type " + macro.getName()
					+ " that is not bound to a macro for binding: " + expanded);
	}

	public static InconsistentBinding undefinedEnvProperty(Name name,
			Type<?> property, Class<?> ns) {
		return new InconsistentBinding(
				"Missing environment property: "
					+ name + " of type " + property + " in " + ns);
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

	public static InconsistentBinding referenceLoop(Binding<?> inconsistent,
			Instance<?> linked, Instance<?> bound) {
		return new InconsistentBinding(
				"Detected a self-referential binding: \n\t" + bound + " => "
					+ linked + "\n\t" + inconsistent);
	}

	public static InconsistentBinding noAnnotationModule(Class<?> type) {
		return new InconsistentBinding(
				"Expected at least one annotation used on type that is bound to an annotation module declaration: " + type.getName());
	}

	public static InconsistentBinding noFunctionalInterface(Class<?> type) {
		return new InconsistentBinding(
				"The type must be annotated with @FunctionalInterface to be usable in this role:"
					+ type.getName());
	}

	public static void nonnullThrowsReentranceException(Object field) {
		if (field != null)
			throw contextAlreadyInitialised();
	}
}
