package se.jbee.inject.defaults;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * To be used in combination with {@link se.jbee.inject.binder.Installs} to
 * specify a set of {@link DefaultFeature}s that should be installed.
 */
@Inherited
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface DefaultFeatures {

	/**
	 * @return the set of features to install
	 */
	DefaultFeature[] value();
}
