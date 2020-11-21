/**
 * <p>
 * This package contains abstractions used to configure the binding process.
 * </p>
 *
 * <h2>Software Editions</h2>
 * <p>
 * {@link se.jbee.inject.config.Edition}s can be used to vary the set of
 * installed {@link se.jbee.inject.bind.Bundle}s and {@link
 * se.jbee.inject.bind.Module} by some form of setting starting from the same
 * root {@link se.jbee.inject.bind.Bundle}. The most common use case is to build
 * {@link se.jbee.inject.config.Edition}s of a software application. This
 * technique avoids forking the code-base or branch based on vendor settings.
 * Instead different "virtual" editions of the same software are defined. If a
 * {@link se.jbee.inject.bind.Bundle} or {@link se.jbee.inject.bind.Module} is
 * part of that {@link se.jbee.inject.config.Edition} is checked during the
 * bootstrapping of the application where the target {@link
 * se.jbee.inject.config.Edition} is defined in the {@link se.jbee.inject.Env}
 * used.
 * </p>
 * <p>
 * One way to select members of an {@link se.jbee.inject.config.Edition} are
 * {@link se.jbee.inject.config.Feature} {@link java.lang.Enum}s.
 * </p>
 *
 * <h2>Customising Binding Backend</h2>
 * <p>
 * The second way to configure the binding process is through the use of
 * "mirrors". These are pure functions that - when using "automatic" (not
 * explicit or reflective) binding - customise which {@link
 * java.lang.reflect.Constructor}s are used, which {@link
 * java.lang.reflect.Method}s are bound as factory, which {@link
 * se.jbee.inject.Scope} is used, which {@link se.jbee.inject.Name} is an {@link
 * se.jbee.inject.Instance} is given or which {@link se.jbee.inject.Hint}s are
 * given for {@link java.lang.reflect.Constructor} or {@link
 * java.lang.reflect.Method} {@link java.lang.reflect.Parameter}s.
 * <p>
 * The defaults for these "mirror" functions is part of the initial {@link
 * se.jbee.inject.Env}. To customise these in general override them in the
 * {@link se.jbee.inject.Env} that is used to bootstrap the {@link
 * se.jbee.inject.Injector}.
 * <p>
 * To override "mirrors" locally within a {@link se.jbee.inject.bind.Module} or
 * binding use {@link se.jbee.inject.binder.BinderModule#autobind()} and the
 * {@link se.jbee.inject.binder.Binder.AutoBinder} methods. These only replace
 * them for the currently used fluent API binder.
 *
 * <h2>Ad-hoc Extension</h2>
 * <p>
 * An {@link se.jbee.inject.config.Extension} is a marker interface to mark
 * "wrapper" classes that should be instantiated and managed ad-hoc (without
 * being bound before). They become a container singleton per exact type.
 * </p>
 * <p>
 * There are 2 usages of this extension mechanism:
 * </p>
 * <ul>
 * <li>{@link se.jbee.inject.config.Config}: Access to configuration value with
 * name-space support and basic conversion and convenience functions.</li>
 * <li>{@link se.jbee.inject.config.Plugins}: Allows convenient access to
 * {@link se.jbee.inject.Resource}s made by the
 * {@link se.jbee.inject.binder.Binder.PluginBinder}.</li>
 * </ul>
 */
package se.jbee.inject.config;
