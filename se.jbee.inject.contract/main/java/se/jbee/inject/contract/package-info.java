/**
 * Contains the add-on to do asynchronous message dispatch build on vanilla Java
 * methods.
 *
 * <h2>Registering Handlers</h2>
 * Any vanilla java interface can be registered as an event handler using {@link
 * se.jbee.inject.contract.ContractModule#handle(java.lang.Class)}.
 *
 * <h2>Sending Events</h2>
 * The event handler interface is injected into the calling managed instance and
 * the handler method is called.
 *
 * <h2>Receiving Events</h2>
 * Any bound and created instance that implements a registered event handler
 * interface will asynchronously receive any message send. Either make sure the
 * instance is injected during bootstrapping of the application or bind the
 * instance in an {@link se.jbee.inject.ScopeLifeCycle#eager()} scope.
 */
package se.jbee.inject.contract;
