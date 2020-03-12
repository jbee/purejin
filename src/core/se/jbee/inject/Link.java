package se.jbee.inject;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ServiceLoader;

/**
 * {@link Annotation} used in connection with {@link ServiceLoader} mechanism to
 * annotate service classes to point out the role they implement in cases where
 * this is ambiguous for the service interface they implement.
 * 
 * Usages:
 * 
 * Annotate a {@link se.jbee.inject.declare.ModuleWith} implementation that
 * should load via {@link ServiceLoader} to implement the effects of an type
 * level {@link Annotation} and have {@link Link#to()} point to the
 * {@link Annotation} type that trigger the implementation module. If the
 * provided {@link Class} is not an {@link Annotation} type it is ignored.
 * 
 * @since 19.1
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface Link {

	Class<?> to();
}
