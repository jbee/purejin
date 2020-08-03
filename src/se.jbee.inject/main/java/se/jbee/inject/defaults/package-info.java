/**
 * <h2>Defaults</h2>
 * <p>
 * This also includes default implementations for
 * {@link se.jbee.inject.bind.ValueBinder}s (see
 * {@link se.jbee.inject.defaults.DefaultValueBinders}) and the
 * {@link se.jbee.inject.Supplier} implementations they use (see
 * {@link se.jbee.inject.binder.Supply}) as well as source value types expended by
 * the default {@link se.jbee.inject.bind.ValueBinder}s:
 * </p>
 * <ul>
 * <li>{@link se.jbee.inject.binder.New}</li>
 * <li>{@link se.jbee.inject.binder.Constant}</li>
 * <li>{@link se.jbee.inject.binder.Produces}</li>
 * </ul>
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
 **/
package se.jbee.inject.defaults;
