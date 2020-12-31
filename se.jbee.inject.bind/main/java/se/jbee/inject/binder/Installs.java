package se.jbee.inject.binder;

import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.Dependent;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A little helper annotation that should be used with caution.
 * <p>
 * Its main purpose is to be used when writing a {@link BinderModule} or {@link
 * BinderModuleWith} which depends upon other {@link Bundle}s or {@link
 * Dependent} bundles being installed. In such a case the
 * {@link BinderModule} or {@link BinderModuleWith} can be annotated instead
 * referencing to its "dependencies" which avoid needing to create a bundle that
 * installs the module and the dependencies.
 * <p>
 * This makes use of the fact that a {@link BinderModule} or {@link
 * BinderModuleWith} also is a {@link Bundle} because it *can* be used as such.
 * In case of {@link Installs} it *must* be used as such in order for the
 * annotation to work. That means it must be installed as a {@link Bundle} using
 * its {@link Class} reference. It can not be installed as a {@link
 * se.jbee.inject.bind.Module} providing an instance. So this does not work for
 * stateful {@link se.jbee.inject.bind.Module}s.
 *
 * @since 8.1
 */
@Inherited
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface Installs {

	/**
	 * @return A set of {@link Bundle}s that is installed when the type
	 * annotated with this annotation is installed as a {@link Bundle}!
	 */
	Class<? extends Bundle>[] bundles() default {};

	/**
	 * @return Must be a {@link Dependent} {@link Enum} type.
	 * If {@link #selection()} is empty all its features are installed.
	 * Otherwise only the features named in {@link #selection()} are installed.
	 */
	Class<? extends Enum> features() default Enum.class;

	/**
	 * @return When {@link #features()} refers to a {@link Dependent}
	 * {@link Enum} type the selection contains the enum constant names of the
	 * features that should be installed. If all should be installed this can be
	 * empty (default value).
	 */
	String[] selection() default {};
}
