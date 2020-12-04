/**
 * <p>
 * This package contains {@link se.jbee.inject.bind.Bundle} implementations
 * which allow to pick up application level {@link se.jbee.inject.bind.Bundle}s
 * using java's {@link java.util.ServiceLoader} mechanism.
 * </p>
 *
 * <h2>Extending the Injector Context</h2>
 * <p>
 * To pick up {@link se.jbee.inject.bind.Bundle}s defined via {@link
 * java.util.ServiceLoader} as part of the {@link se.jbee.inject.Injector}
 * context install the {@link se.jbee.inject.binder.ServiceLoaderBundles} bundle
 * in one of your application's {@link se.jbee.inject.bind.Bundle}s when
 * creating the {@link se.jbee.inject.Injector} context.
 * </p>
 *
 * <h2>Extending the Env Context</h2>
 * <p>
 * To pick up {@link se.jbee.inject.bind.Bundle}s defined via {@link
 * java.util.ServiceLoader} as part of the {@link se.jbee.inject.Env} context
 * install the {@link se.jbee.inject.binder.ServiceLoaderEnvBundles} bundle in
 * one of your application's environment {@link se.jbee.inject.bind.Bundle}s
 * when creating the {@link se.jbee.inject.Env} context.
 * </p>
 *
 * <h2>Adding Custom Type Level Annotation Definitions</h2>
 * <p>
 * To pick up {@link se.jbee.inject.bind.ModuleWith} defining the effects of an
 * particular {@link java.lang.annotation.Annotation} install the {@link
 * se.jbee.inject.binder.ServiceLoaderAnnotations} {@link
 * se.jbee.inject.bind.Module} as part of the {@link se.jbee.inject.Env}
 * context.
 * </p>
 */
package se.jbee.inject.binder;
