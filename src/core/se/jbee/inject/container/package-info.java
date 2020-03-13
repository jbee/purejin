/**
 * This package contains the default implementation for the
 * {@link se.jbee.inject.Injector} context. It is created from a list of
 * {@link se.jbee.inject.container.Injectee}s.
 * 
 * The implementation is independent of the
 * {@link se.jbee.inject.declare.Binding} based API which is build on top of
 * {@link se.jbee.inject.container.Injectee}, the
 * {@link se.jbee.inject.bootstrap.Bootstrap} utility which builds on top of the
 * {@link se.jbee.inject.declare.Binding} API and the utility
 * {@link se.jbee.inject.declare.Module} and
 * {@link se.jbee.inject.declare.Bundle} implementations found in the fluent
 * high level API of {@link se.jbee.inject.bind.BinderModule} and
 * {@link se.jbee.inject.bind.BootstrapperBundle}.
 * 
 * Many default features of the {@link se.jbee.inject.Injector} are actually
 * defined in user space in form of {@link se.jbee.inject.declare.Module}s. For
 * example {@link se.jbee.inject.Scope}s are usual
 * {@link se.jbee.inject.Resource}s that are just bound to their implementations
 * by default by the {@link se.jbee.inject.bind.DefaultScopes}
 * {@link se.jbee.inject.declare.Module}.
 */
package se.jbee.inject.container;