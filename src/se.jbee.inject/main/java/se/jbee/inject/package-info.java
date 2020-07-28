/**
 * <p>
 * The root package contains all essential concepts and types of the library.
 * The user facing API is the {@link se.jbee.inject.Injector}.
 * </p>
 * 
 * <h2>Code organisation</h2>
 * <p>
 * The root package with it essential types does not depend on any of its
 * sub-packages, the sub-packages depend on the root.
 * </p>
 * <p>
 * Dependency-wise each sub-packages represents a layer within the code-base.
 * That means there is an order in which they extend each other and the
 * functionality provided by all sub-packages in a lower layer. None of the
 * packages have cyclic dependencies on each other.
 * </p>
 * 
 * <h2>Sub-Packages</h2>
 * <ul>
 * <li><b>container</b>: Contains the {@link se.jbee.inject.Injector}
 * implementation {@link se.jbee.inject.container.Container}</li>
 * <li><b>scope</b>: Contains the {@link se.jbee.inject.Scope} implementations.
 * {@link se.jbee.inject.Scope}s are bound as defaults in user space an can be
 * fully customised.</li>
 * <li><b>declare</b>: Defines the low level API to bootstrap and describe the
 * contents of a {@link se.jbee.inject.Injector} context.</li>
 * <li><b>bootstrap</b>: Contains the default implementation to
 * {@link se.jbee.inject.bootstrap.Bootstrap} an {@link se.jbee.inject.Injector}
 * context from a root {@link se.jbee.inject.declare.Bundle}.</li>
 * <li><b>bind</b>: Contains the high level fluent API to make bindings.</li>
 * <li><b>config</b>: Defines the abstractions API that can be used to configure
 * the bootstrapping process.</li>
 * <li><b>extend</b>: Defines the abstraction API for
 * {@link se.jbee.inject.extend.Extension}s and implementations for convenient
 * access to {@link se.jbee.inject.extend.Plugins} and application level
 * {@link se.jbee.inject.extend.Config}s.</li>
 * <li><b>action</b>: Contains a level utility for loosely coupled application
 * composition on the basis of unique parameter types.</li>
 * <li><b>event</b>: Contains an experimental high level utility for event based
 * application level execution.</li>
 * </ul>
 */
package se.jbee.inject;