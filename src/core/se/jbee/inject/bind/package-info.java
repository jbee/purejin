/**
 * This package provides the high level fluent
 * {@link se.jbee.inject.bind.Binder} API.
 * 
 * For {@link se.jbee.inject.declare.Module}s use the
 * {@link se.jbee.inject.bind.BinderModule} as default; for
 * {@link se.jbee.inject.declare.Bundle}s use the
 * {@link se.jbee.inject.bind.BootstrapperBundle} as default.
 * 
 * {@link se.jbee.inject.declare.ModuleWith} is usually implemented using the
 * {@link se.jbee.inject.bind.BinderModuleWith} API.
 * 
 * The named defaults above will all install the
 * {@link se.jbee.inject.bind.DefaultsBundle} which binds defaults for
 * {@link se.jbee.inject.Scope}s, {@link se.jbee.inject.AnnotatedWith} and
 * {@link se.jbee.inject.SPI}. As always this can be uninstalled or overridden
 * using explicit binds.
 * 
 * The {@link se.jbee.inject.bind.AnnotatedWithModule} provides the default
 * implementation of the {@link se.jbee.inject.AnnotatedWith} utility type.
 * 
 * The {@link se.jbee.inject.bind.SPIModule} provides the default implementation
 * for {@link se.jbee.inject.SPI} concept.
 * 
 * The {@link se.jbee.inject.bind.Adapter}
 * {@link se.jbee.inject.bind.TogglerBundle} provides type level adapters.
 * {@link se.jbee.inject.bind.Adapter#ENV} and
 * {@link se.jbee.inject.bind.Adapter#SUB_CONTEXT} are installed by default.
 * 
 * The {@link se.jbee.inject.bind.EnvModule} provides a base
 * {@link se.jbee.inject.declare.Module} that is suitable to extend the
 * {@link se.jbee.inject.Env} context in case it is bootstrapped from
 * {@link se.jbee.inject.declare.Bundle}s as well.
 */
package se.jbee.inject.bind;