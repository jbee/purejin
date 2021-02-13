/**
 * Contains the core utility to {@link se.jbee.inject.bootstrap.Bootstrap} a
 * {@link se.jbee.inject.Injector} or {@link se.jbee.inject.Env} from a root
 * {@link se.jbee.inject.bind.Bundle}.
 *
 * <h2>Custom Environments</h2>
 * <p>
 * The {@link se.jbee.inject.bootstrap.Bootstrap#DEFAULT_ENV} is a default
 * implementation of {@link se.jbee.inject.Env} in form of a map. More complex
 * setup can be done by bootstrapping the {@link se.jbee.inject.Env} itself from
 * a root {@link se.jbee.inject.bind.Bundle} and convert the resulting {@link
 * se.jbee.inject.Injector} to an {@link se.jbee.inject.Env} using {@link
 * se.jbee.inject.Injector#asEnv()}. Most often such a setup will use {@link
 * se.jbee.inject.Env#with(se.jbee.inject.Name, Type,
 * java.lang.Object)} to add properties to the resulting environment that are
 * only available at runtime like command line arguments.
 * </p>
 */
package se.jbee.inject.bootstrap;

import se.jbee.lang.Type;
