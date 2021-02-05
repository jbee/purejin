package se.jbee.inject.event;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface On {

	interface Aware {}

	enum DispatchType {
		/**
		 * Returned events are first processed when all receivers of the
		 * currently processed event have received it.
		 */
		SEQUENTIAL,
		/**
		 * Returned events are processed directly after they have been returned.
		 * This might be before further receivers receive the currently
		 * processed event or after.
		 */
		INTERLEAVED
	}

	/**
	 * An event triggered when the JVM is shutting down.
	 */
	class Shutdown {}

	Class<?>[] value();

	DispatchType proceed() default DispatchType.SEQUENTIAL;
}
