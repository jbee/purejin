/**
 * <p>
 * This package contains the default implementation for the
 * {@link se.jbee.inject.Injector} context. It is created from a list of
 * {@link se.jbee.inject.ResourceDescriptor}s. Once a context is created the
 * list of {@link se.jbee.inject.Resource}s it contains it immutable. This means
 * the type of things it knows how to resolve is fixed.
 * </p>
 *
 * <h2>{@link se.jbee.inject.Injector} Features</h2>
 * <p>
 * Many default features of the {@link se.jbee.inject.Injector} are actually
 * defined in user space in form of {@link se.jbee.inject.bind.Module}s. For
 * example {@link se.jbee.inject.Scope}s are usual
 * {@link se.jbee.inject.Resource}s that are bound to their implementations by
 * default by the {@link se.jbee.inject.defaults.DefaultScopes}
 * {@link se.jbee.inject.bind.Module}.
 * </p>
 *
 * <h2>A Note on Independence</h2>
 * <p>
 * The {@link se.jbee.inject.container.Container} implementation is independent
 * of the {@link se.jbee.inject.bind.Binding} based API which is build on top
 * of {@link se.jbee.inject.ResourceDescriptor}, the
 * {@link se.jbee.inject.bootstrap.Bootstrap} utility which builds on top of the
 * {@link se.jbee.inject.bind.Binding} API and the utility
 * {@link se.jbee.inject.bind.Module} and
 * {@link se.jbee.inject.bind.Bundle} implementations found in the fluent
 * high level API of {@link se.jbee.inject.binder.BinderModule} and
 * {@link se.jbee.inject.binder.BootstrapperBundle}.
 * </p>
 */
package se.jbee.inject.container;
