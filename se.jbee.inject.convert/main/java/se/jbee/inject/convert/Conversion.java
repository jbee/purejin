package se.jbee.inject.convert;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A marker annotation for {@link java.lang.reflect.Method}s that should be used
 * as a {@link se.jbee.inject.Converter}.
 */
@Inherited
@Documented
@Retention(RUNTIME)
@Target({METHOD})
public @interface Conversion {

	/**
	 * Marker interface to be implemented by managed instances that use the
	 * {@link Conversion} annotation to mark methods that should be used as
	 * {@link se.jbee.inject.Converter}.
	 */
	interface Aware {}

	/**
	 * @return index of the annotated method parameter that is the input of the
	 * conversion.
	 */
	int fromIndex() default 0;
}
