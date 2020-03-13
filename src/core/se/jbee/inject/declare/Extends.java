package se.jbee.inject.declare;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ServiceLoader;

import se.jbee.inject.Env;
import se.jbee.inject.Injector;

/**
 * {@link Annotation} used in connection with {@link ServiceLoader} mechanism to
 * annotate service classes to point out the role they implement in cases where
 * this is ambiguous for the service interface they implement.
 * 
 * Usages:
 * 
 * Annotate a {@link se.jbee.inject.declare.ModuleWith} implementation that
 * should load via {@link ServiceLoader} to implement the effects of an type
 * level {@link Annotation} and have {@link Extends#value()} point to the
 * {@link Annotation} type that triggers the implementation
 * {@link se.jbee.inject.declare.ModuleWith}. If the provided {@link Class} is
 * not an {@link Annotation} type the module is ignored.
 * 
 * Annotate a {@link se.jbee.inject.declare.Bundle} implementation that should
 * load via {@link ServiceLoader} have {@link Extends#value()} be {@link Env}
 * {@link Class} to add the bundle to those that should be loaded as part of the
 * {@link Env} instead of the {@link Injector} context.
 * 
 * @since 19.1
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface Extends {

	Class<?> value();
}
