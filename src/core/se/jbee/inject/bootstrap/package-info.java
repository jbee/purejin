/**
 * <h3>Summary</h3>
 * <p>
 * This package contains core utility to
 * {@link se.jbee.inject.bootstrap.Bootstrap} a {@link se.jbee.inject.Injector}
 * from a root {@link se.jbee.inject.declare.Bundle}.
 * </p>
 * 
 * <h3>Custom Environments</h3>
 * <p>
 * The {@link se.jbee.inject.bootstrap.Environment} is a default implementation
 * of {@link se.jbee.inject.Env} in form of a map. More complex setup can be
 * done by bootstrapping the {@link se.jbee.inject.Env} itself from a root
 * {@link se.jbee.inject.declare.Bundle} and convert the resulting
 * {@link se.jbee.inject.Injector} to an {@link se.jbee.inject.Env} using
 * {@link se.jbee.inject.Injector#asEnv()}. Most often such a setup will use
 * {@link se.jbee.inject.bootstrap.Environment#complete(se.jbee.inject.Env)} to
 * add properties to the resulting environment that are only available at
 * runtime like command line arguments.
 * </p>
 * 
 * <h3>Defaults</h3>
 * <p>
 * This includes default implementations for
 * {@link se.jbee.inject.declare.ValueBinder}s (see
 * {@link se.jbee.inject.bootstrap.DefaultBinders}) and the
 * {@link se.jbee.inject.container.Supplier} implementations they use (see
 * {@link se.jbee.inject.bootstrap.Supply}) as well as source value types
 * expended by the default {@link se.jbee.inject.declare.ValueBinder}s:
 * <ul>
 * <li>{@link se.jbee.inject.bootstrap.New}</li>
 * <li>{@link se.jbee.inject.bootstrap.Constant}</li>
 * <li>{@link se.jbee.inject.bootstrap.Produces}</li>
 * </ul>
 * </p>
 */
package se.jbee.inject.bootstrap;