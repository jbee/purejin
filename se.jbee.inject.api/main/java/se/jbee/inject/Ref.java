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
public interface Ref { // alternative name: Descriptor

	/* just a marker */


	final class BridgeRef implements Ref {

		public final Class<?> type;

		public BridgeRef(Class<?> type) {
			this.type = type;
		}
	}

	final class ArrayRef implements Ref {
		public final Hint<?>[] elements;

		public ArrayRef(Hint<?>[] elements) {
			this.elements = elements;
		}
	}
}
