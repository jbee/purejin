/**
 * <h3>Summary</h3>
 * <p>
 * This package contains the core API to create
 * {@link se.jbee.inject.declare.Binding}s as a source of
 * {@link se.jbee.inject.container.Injectee}s to create a
 * {@link se.jbee.inject.Injector} context from.
 * </p>
 * 
 * <h3>API</h3>
 * <p>
 * The core element of composition is a {@link se.jbee.inject.declare.Bundle}.
 * Its task is to model the application's composition tree.
 * {@link se.jbee.inject.declare.Module}s are the leaves on this tree. They bind
 * {@link se.jbee.inject.Locator}s to {@link se.jbee.inject.Resource}s. A list
 * of these effectively makes the {@link se.jbee.inject.Injector} context.
 * </p>
 * <p>
 * When the {@link se.jbee.inject.Resource} bound depends on a configuration or
 * program parameter a {@link se.jbee.inject.declare.ModuleWith} can be used to
 * conveniently resolve the value from the {@link se.jbee.inject.Env} by its
 * type.
 * </p>
 * <p>
 * {@link se.jbee.inject.declare.Toggled} {@link java.lang.Enum}s can be used to
 * implement feature toggles where a {@link se.jbee.inject.declare.Bundle} is
 * only installed if the {@link java.lang.Enum} constant associated with the
 * {@link se.jbee.inject.declare.Bundle} is
 * {@link se.jbee.inject.Env#toggled(Class, Enum, Package)}. Besides being
 * useful as feature switch this also allows to hide actual
 * {@link se.jbee.inject.declare.Bundle} implementation classes and make them
 * accessible through a {@link java.lang.Enum} and its constants. This is the
 * equivalent of an interface for {@link se.jbee.inject.declare.Bundle} classes.
 * </p>
 */
package se.jbee.inject.declare;