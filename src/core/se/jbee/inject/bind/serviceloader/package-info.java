/**
 * <h3>Summary</h3>
 * <p>
 * This package contains {@link se.jbee.inject.declare.Bundle} implementations
 * which allow to pick up application level
 * {@link se.jbee.inject.declare.Bundle}s using java's
 * {@link java.util.ServiceLoader} mechanism.
 * </p>
 * 
 * <h3>Extending the Injector Context</h3>
 * <p>
 * To pick up {@link se.jbee.inject.declare.Bundle}s defined via
 * {@link java.util.ServiceLoader} as part of the
 * {@link se.jbee.inject.Injector} context install the
 * {@link se.jbee.inject.bind.serviceloader.ServiceLoaderBundles} bundle in one
 * of your applications {@link se.jbee.inject.declare.Bundle}s. That is one
 * installed when expanding the root {@link se.jbee.inject.declare.Bundle} of
 * {@link se.jbee.inject.bootstrap.Bootstrap#injector(se.jbee.inject.Env,
 * Class).}
 * </p>
 * 
 * <h3>Extending the Env Context</h3>
 * <p>
 * To pick up {@link se.jbee.inject.declare.Bundle}s defined via
 * {@link java.util.ServiceLoader} as part of the {@link se.jbee.inject.Env}
 * context install the
 * {@link se.jbee.inject.bind.serviceloader.ServiceLoaderEnvBundles} bundle in
 * one of your applications environment {@link se.jbee.inject.declare.Bundle}s.
 * That is one install when expanding the root
 * {@link se.jbee.inject.declare.Bundle} of
 * {@link se.jbee.inject.bootstrap.Bootstrap#env(Class)}.
 * </p>
 * 
 * <h3>Adding Custom Type Level Annotation Definitions</h3>
 * <p>
 * To pick up {@link se.jbee.inject.declare.ModuleWith} defining the effects of
 * an particular {@link java.lang.annotation.Annotation} install the
 * {@link se.jbee.inject.bind.serviceloader.ServiceLoaderAnnotations}
 * {@link se.jbee.inject.declare.Module} as part of the
 * {@link se.jbee.inject.Env} context.
 * </p>
 */
package se.jbee.inject.bind.serviceloader;