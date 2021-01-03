package se.jbee.inject;

import java.lang.annotation.Target;
import java.lang.annotation.*;
import java.util.ServiceLoader;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@link Annotation} used in connection with {@link ServiceLoader} mechanism to
 * annotate service classes to point out the role they implement in cases where
 * this is ambiguous for the service interface they implement.
 *
 * <h2>Usages</h2>
 *
 * <h3>Providing Annotation Templet via ServiceLoader</h3>
 * <p>
 * Annotate a {@code se.jbee.inject.bind.ModuleWith} implementation that should
 * load via {@link ServiceLoader} to implement the effects of an type level
 * {@link Annotation} and have {@link Extends#value()} point to the
 * {@link Annotation} type that triggers the implementation
 * {@code se.jbee.inject.bind.ModuleWith}. If the provided {@link Class} is not
 * an {@link Annotation} type the module is ignored.
 * </p>
 *
 * <h3>Providing Bundles via ServiceLoader</h3>
 * <p>
 * Annotate a {@code se.jbee.inject.bind.Bundle} implementation that should load
 * via {@link ServiceLoader} have {@link Extends#value()} be {@link Env}
 * {@link Class} to add the bundle to those that should be loaded as part of the
 * {@link Env} instead of the {@link Injector} context. To add
 * {@code se.jbee.inject.bind.Bundle}s to the {@link Injector} context don't
 * annotate them with {@link Extends} or use {@link Injector} {@link Class} as
 * {@link Extends#value()}.
 * <p>
 *
 * @since 8.1
 */
@Inherited
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface Extends {

	/**
	 * @return refers to a type in the context of the annotated extension. The
	 *         semantic depends on the annotated type. See class level
	 *         documentation for details.
	 */
	Class<?> value();

}
