/**
 * <p>
 * This package contains the default implementation for the {@link
 * se.jbee.inject.Injector} context. It is created from a list of {@link
 * se.jbee.inject.ResourceDescriptor}s. Once a context is created the list of
 * {@link se.jbee.inject.Resource}s it contains it immutable. This means the
 * type of things it knows how to resolve is fixed.
 * </p>
 *
 * <h2>{@link se.jbee.inject.Injector} Features</h2>
 * <p>
 * Many of the "basic" features of the {@link se.jbee.inject.Injector} are actually
 * defined in user space in form of {@code se.jbee.inject.bind.Module}s that make
 * bindings to produce {@link se.jbee.inject.ResourceDescriptor}s.
 *
 * For example {@link se.jbee.inject.Scope}s are usual {@link
 * se.jbee.inject.Resource}s that are bound to their implementations by default
 * by the {@code se.jbee.inject.defaults.DefaultScopes} module.
 * </p>
 *
 * <h2>A Note on Independence</h2>
 * <p>
 * The {@link se.jbee.inject.container.Container} implementation is independent
 * of the {@code se.jbee.inject.bind.Binding} based fluent API. Its bindings
 * extends the {@link se.jbee.inject.ResourceDescriptor} type that is the basis
 * of creating the actual container implementation.
 * </p>
 */
package se.jbee.inject.container;
