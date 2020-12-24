package se.jbee.inject.convert;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Is added to {@link se.jbee.inject.Converter}s to add additional input value
 * transformation chains to extend the possible input types of a particular
 * {@link se.jbee.inject.Converter}.
 */
@Documented
@Repeatable(ConvertsMultiple.class)
@Retention(RUNTIME)
@Target({ TYPE, METHOD, CONSTRUCTOR, FIELD })
public @interface Converts {

	/**
	 * @return A conversion sequence, for example {@code A, B, C} means a value
	 * of type {@code A} is first converted to {@code B} and from {@code B} to
	 * {@code C}. The final type (here {@code C}) should be the input type of
	 * the {@link se.jbee.inject.Converter} this annotation is attached to.
	 * <p>
	 * By adding such additional conversion a {@link se.jbee.inject.Converter}
	 * with an input of {@code C} then can also be have inputs of type {@code A}
	 * or {@code B} as we know how to get from those to the required input type
	 * {@code C}.
	 */
	String[] value();

	/**
	 * @return The set of {@link Class} to import so that their {@link
	 * Class#getSimpleName()} can be used in the conversion sequence given by
	 * {@link #value()}. Alternativly (or in addition) the {@link Imports}
	 * annotation can be used to provide the full {@link Class} references for
	 * simple class names.
	 */
	Class<?>[] imports() default {};
}
