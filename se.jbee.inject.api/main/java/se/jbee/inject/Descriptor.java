package se.jbee.inject;

/**
 * A marker interface for types that can be used as a reference to a particular
 * way to create bindings.
 * <p>
 * It is in some sense a source value that can be turned into one or more
 * bindings during the binding process.
 * <p>
 * The reason this is defined in the core is simply that some of the core types
 * should implement it. Otherwise this would be found in the binding module.
 *
 * @since 8.1
 */
public interface Descriptor {

	/* just a marker */


	final class BridgeDescriptor implements Descriptor {

		public final Class<?> type;

		public BridgeDescriptor(Class<?> type) {
			this.type = type;
		}
	}

	final class ArrayDescriptor implements Descriptor {
		public final Hint<?>[] elements;

		public ArrayDescriptor(Hint<?>[] elements) {
			this.elements = elements;
		}
	}
}
