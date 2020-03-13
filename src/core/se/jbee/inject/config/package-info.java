/**
 * <h3>Summary</h3>
 * <p>
 * This package contains abstractions used to configure the binding process.
 * </p>
 * 
 * <h3>Software Editions</h3>
 * <p>
 * {@link se.jbee.inject.config.Edition}s can be used to vary the set of
 * installed {@link se.jbee.inject.declare.Bundle}s and
 * {@link se.jbee.inject.declare.Module} by some form of setting starting from
 * the same root {@link se.jbee.inject.declare.Bundle}. The most common use case
 * is to build {@link se.jbee.inject.config.Edition}s of a software application.
 * This technique avoids forking the code-base or branch based on vendor
 * settings. Instead different "virtual" editions of the same software are
 * defined. If a {@link se.jbee.inject.declare.Bundle} or
 * {@link se.jbee.inject.declare.Module} is part of that
 * {@link se.jbee.inject.config.Edition} is checked during the bootstrapping of
 * the application where the target {@link se.jbee.inject.config.Edition} is
 * defined in the {@link se.jbee.inject.Env} used.
 * </p>
 * <p>
 * One way to select members of an {@link se.jbee.inject.config.Edition} are
 * {@link se.jbee.inject.config.Feature} {@link java.lang.Enum}s.
 * </p>
 * 
 * <h3>Customising Binding Backend</h3>
 * <p>
 * The second way to configure the binding process is through the use of
 * "mirrors". These are pure functions that - when using "automatic" (not
 * explicit or reflective) binding - customise which
 * {@link java.lang.reflect.Constructor}s are used, which
 * {@link java.lang.reflect.Method}s are bound as factory, which
 * {@link se.jbee.inject.Scope} is used, which {@link se.jbee.inject.Name}
 * automatic {@link se.jbee.inject.Resource} {@link se.jbee.inject.Instance}s
 * get or which {@link se.jbee.inject.Hint} is given for
 * {@link java.lang.reflect.Constructor} or {@link java.lang.reflect.Method}
 * {@link java.lang.reflect.Parameter}s. Their default is set by
 * {@link se.jbee.inject.bootstrap.Bootstrap#ENV}. To customise these in general
 * override them in the {@link se.jbee.inject.Env} used to
 * {@link se.jbee.inject.bootstrap.Bootstrap} the
 * {@link se.jbee.inject.Injector}. To override them locally use
 * {@link se.jbee.inject.bind.BinderModule#autobind()} and the
 * {@link se.jbee.inject.bind.Binder.AutoBinder} methods. These only replace
 * them for the currently used fluent API binder.
 * </p>
 */
package se.jbee.inject.config;