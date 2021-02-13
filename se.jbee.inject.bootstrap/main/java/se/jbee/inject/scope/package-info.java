/**
 * <p>
 * Contains the default implementations of common {@link se.jbee.inject.Scope}s.
 * In the library a scope implementation is a managed container instance.
 * </p>
 *
 * <h2>Defaults</h2>
 * <p>
 * The {@link se.jbee.inject.defaults.DefaultScopes} {@link
 * se.jbee.inject.bind.Module} is used to define the common {@link
 * se.jbee.inject.Scope} implementations.
 * </p>
 *
 * <h2>Provided Scopes</h2>
 * <ul>
 * <li>{@link se.jbee.inject.scope.ApplicationScope}: A singleton within the
 * {@link se.jbee.inject.Injector} context.</li>
 * <li>{@link se.jbee.inject.scope.ThreadScope}: A singleton per JVM
 * {@link java.lang.Thread}</li>
 * <li>{@link se.jbee.inject.scope.WorkerScope}: Base implementation for
 * {@link java.lang.Thread} pool based scopes like a request scope in an HTTP
 * server.</li>
 * <li>{@link se.jbee.inject.scope.DiskScope}: {@link java.io.File} based scope
 * for {@link java.io.Serializable} values in a particular directory.</li>
 * <li>{@link se.jbee.inject.scope.TypeDependentScope}: Implementation for
 * singletons per resolved {@link java.lang.Class}, {@link se.jbee.lang.Type},
 * {@link se.jbee.inject.Instance} or full
 * {@link se.jbee.inject.Dependency}.</li>
 * <li>{@link se.jbee.inject.scope.SnapshotScope}: A utility
 * {@link se.jbee.inject.Scope} that allows to create effective snapshots of
 * other {@link se.jbee.inject.Scope} that change asynchronously or concurrently
 * to another one, like e.g. a {@link se.jbee.inject.scope.DiskScope}.</li>
 * </ul>
 */
package se.jbee.inject.scope;
