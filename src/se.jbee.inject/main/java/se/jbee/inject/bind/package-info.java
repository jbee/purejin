/**
 * <h3>Summary</h3>
 * <p>
 * This package provides the high level fluent
 * {@link se.jbee.inject.bind.Binder} API.
 * </p>
 * 
 * <h3>Usage</h3>
 * <p>
 * Most notably the fluent API uses effective immutable objects. This means any
 * intermediate state of the fluent API can be "stored" and reused by assigning
 * the returned object to a variable and reusing that variable to get everything
 * set so far without effecting other completions made from this variable or
 * others assigned before.
 * </p>
 * <p>
 * For {@link se.jbee.inject.declare.Module}s use the
 * {@link se.jbee.inject.bind.BinderModule} as default; for
 * {@link se.jbee.inject.declare.Bundle}s use the
 * {@link se.jbee.inject.bind.BootstrapperBundle} as default.
 * </p>
 * <p>
 * {@link se.jbee.inject.declare.ModuleWith} is usually implemented using the
 * {@link se.jbee.inject.bind.BinderModuleWith} API.
 * </p>
 * 
 * <h3>Defaults</h3>
 * <p>
 * The base classes mentioned in usage section all install the
 * {@link se.jbee.inject.bind.DefaultsBundle} which binds defaults for
 * {@link se.jbee.inject.Scope}s, {@link se.jbee.inject.AnnotatedWith} and
 * {@link se.jbee.inject.extend.Extension}. As always this can be uninstalled or
 * overridden using explicit binds.
 * </p>
 * 
 * <h3>Utilities</h3>
 * <p>
 * The {@link se.jbee.inject.bind.AnnotatedWithModule} provides the default
 * implementation of the {@link se.jbee.inject.AnnotatedWith} utility type.
 * </p>
 * <p>
 * The {@link se.jbee.inject.bind.ExtensionModule} provides the default implementation
 * for {@link se.jbee.inject.extend.Extension} concept.
 * </p>
 * <p>
 * The {@link se.jbee.inject.bind.Adapter}
 * {@link se.jbee.inject.bind.TogglerBundle} provides type level adapters.
 * {@link se.jbee.inject.bind.Adapter#ENV} and
 * {@link se.jbee.inject.bind.Adapter#SUB_CONTEXT} are installed by default.
 * </p>
 * 
 * <h3>Custom Environments</h3>
 * <p>
 * The {@link se.jbee.inject.bind.EnvModule} provides a base
 * {@link se.jbee.inject.declare.Module} that is suitable to extend the
 * {@link se.jbee.inject.Env} context in case it is bootstrapped from
 * {@link se.jbee.inject.declare.Bundle}s as well.
 * </p>
 */
package se.jbee.inject.bind;