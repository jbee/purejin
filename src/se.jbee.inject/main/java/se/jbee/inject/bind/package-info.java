/**
 * <p>
 * This package provides the high level fluent
 * {@link se.jbee.inject.binder.Binder} API.
 * </p>
 * 
 * *
 * <p>
 * This package contains the core API to create
 * {@link se.jbee.inject.bind.Binding}s as a source of
 * {@link se.jbee.inject.ResourceDescriptor}s to create a
 * {@link se.jbee.inject.Injector} context from.
 * </p>
 * 
 * <h2>API</h2>
 * <p>
 * The core element of composition is a {@link se.jbee.inject.bind.Bundle}. Its
 * task is to model the application's composition tree.
 * {@link se.jbee.inject.bind.Module}s are the leaves on this tree. They bind
 * {@link se.jbee.inject.Locator}s to {@link se.jbee.inject.Resource}s. A list
 * of these effectively makes the {@link se.jbee.inject.Injector} context.
 * </p>
 * <p>
 * When the {@link se.jbee.inject.Resource} bound depends on a configuration or
 * program parameter a {@link se.jbee.inject.bind.ModuleWith} can be used to
 * conveniently resolve the value from the {@link se.jbee.inject.Env} by its
 * type.
 * </p>
 * <p>
 * {@link se.jbee.inject.bind.Toggled} {@link java.lang.Enum}s can be used to
 * implement feature toggles where a {@link se.jbee.inject.bind.Bundle} is only
 * installed if the {@link java.lang.Enum} constant associated with the
 * {@link se.jbee.inject.bind.Bundle} is
 * {@link se.jbee.inject.Env#toggled(Class, Enum, Package)}. Besides being
 * useful as feature switch this also allows to hide actual
 * {@link se.jbee.inject.bind.Bundle} implementation classes and make them
 * accessible through a {@link java.lang.Enum} and its constants. This is the
 * equivalent of an interface for {@link se.jbee.inject.bind.Bundle} classes.
 * </p>
 * 
 * <h2>Fluent API Usage</h2>
 * <p>
 * Most notably the fluent API uses effective immutable objects. This means any
 * intermediate state of the fluent API can be "stored" and reused by assigning
 * the returned object to a variable and reusing that variable to get everything
 * set so far without effecting other completions made from this variable or
 * others assigned before.
 * </p>
 * <p>
 * For {@link se.jbee.inject.bind.Module}s use the
 * {@link se.jbee.inject.binder.BinderModule} as default; for
 * {@link se.jbee.inject.bind.Bundle}s use the
 * {@link se.jbee.inject.binder.BootstrapperBundle} as default.
 * </p>
 * <p>
 * {@link se.jbee.inject.bind.ModuleWith} is usually implemented using the
 * {@link se.jbee.inject.binder.BinderModuleWith} API.
 * </p>
 * 
 * <h2>Defaults</h2>
 * <p>
 * The base classes mentioned in usage section all install the
 * {@link se.jbee.inject.defaults.DefaultsBundle} which binds defaults for
 * {@link se.jbee.inject.Scope}s, {@link se.jbee.inject.AnnotatedWith} and
 * {@link se.jbee.inject.config.Extension}. As always this can be uninstalled or
 * overridden using explicit binds.
 * </p>
 * 
 * <h2>Utilities</h2>
 * <p>
 * The {@link se.jbee.inject.defaults.AnnotatedWithModule} provides the default
 * implementation of the {@link se.jbee.inject.AnnotatedWith} utility type.
 * </p>
 * <p>
 * The {@link se.jbee.inject.defaults.ExtensionModule} provides the default
 * implementation for {@link se.jbee.inject.config.Extension} concept.
 * </p>
 * <p>
 * The {@link se.jbee.inject.defaults.CoreFeature}
 * {@link se.jbee.inject.binder.TogglerBundle} provides type level adapters.
 * {@link se.jbee.inject.defaults.CoreFeature#ENV} and
 * {@link se.jbee.inject.defaults.CoreFeature#SUB_CONTEXT} are installed by
 * default.
 * </p>
 * 
 * <h2>Custom Environments</h2>
 * <p>
 * The {@link se.jbee.inject.binder.EnvModule} provides a base
 * {@link se.jbee.inject.bind.Module} that is suitable to extend the
 * {@link se.jbee.inject.Env} context in case it is bootstrapped from
 * {@link se.jbee.inject.bind.Bundle}s as well.
 * </p>
 */
package se.jbee.inject.bind;