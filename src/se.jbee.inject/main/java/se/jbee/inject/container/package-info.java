/**
 * <p>
 * This package contains the default implementation for the
 * {@link se.jbee.inject.Injector} context. It is created from a list of
 * {@link se.jbee.inject.container.Injectee}s. Once a context is created the
 * list of {@link se.jbee.inject.Resource}s it contains it immutable. This means
 * the type of things it knows how to resolve is fixed.
 * </p>
 * 
 * <h2>Initialising Objects Created In Context</h2>
 * <p>
 * To initialise objects created in the {@link se.jbee.inject.Injector} bind a
 * {@link se.jbee.inject.container.Initialiser} for the
 * {@link se.jbee.inject.Type} of objects it should initialise. This is mostly
 * equivalent to a <code>javax.annotation.PostConstruct</code> annotation just
 * that the effect is not given by an annotated method but by the provided
 * {@link se.jbee.inject.container.Initialiser} function. This can equally be
 * applied to the {@link se.jbee.inject.Injector} itself to wrap it.
 * {@link se.jbee.inject.container.Container#injector(Injectee...)} will return
 * the outermost wrapper in that case.
 * </p>
 * 
 * <h2>{@link se.jbee.inject.Injector} Features</h2>
 * <p>
 * Many default features of the {@link se.jbee.inject.Injector} are actually
 * defined in user space in form of {@link se.jbee.inject.declare.Module}s. For
 * example {@link se.jbee.inject.Scope}s are usual
 * {@link se.jbee.inject.Resource}s that are bound to their implementations by
 * default by the {@link se.jbee.inject.bind.DefaultScopes}
 * {@link se.jbee.inject.declare.Module}.
 * </p>
 * 
 * <h2>A Note on Independence</h2>
 * <p>
 * The {@link se.jbee.inject.container.Container} implementation is independent
 * of the {@link se.jbee.inject.declare.Binding} based API which is build on top
 * of {@link se.jbee.inject.container.Injectee}, the
 * {@link se.jbee.inject.bootstrap.Bootstrap} utility which builds on top of the
 * {@link se.jbee.inject.declare.Binding} API and the utility
 * {@link se.jbee.inject.declare.Module} and
 * {@link se.jbee.inject.declare.Bundle} implementations found in the fluent
 * high level API of {@link se.jbee.inject.bind.BinderModule} and
 * {@link se.jbee.inject.bind.BootstrapperBundle}.
 * </p>
 */
package se.jbee.inject.container;