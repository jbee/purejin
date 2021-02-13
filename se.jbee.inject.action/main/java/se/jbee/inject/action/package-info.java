/**
 * Contains the {@link se.jbee.inject.action.Action} add-on implementation.
 * <p>
 * {@link se.jbee.inject.action.Action}s are abstractions for vanilla Java
 * methods which are identified by their parameter and return type pair.
 * <p>
 * Callers can inject them as {@link se.jbee.inject.action.Action} without
 * becoming dependent on the provider of the underlying implementation. This
 * allows a very loosely coupled highly modularised application composed out of
 * modules that do not need to know each other. The contract between the
 * application module are the data types of parameter and return types.
 * <p>
 * {@link se.jbee.inject.action.Action}s can also be used do emulate method
 * interception without bytecode manipulation. All calls to {@link
 * se.jbee.inject.action.Action}s are executed using the {@link
 * se.jbee.inject.action.ActionExecutor}. Any custom interception can be implemented
 * by providing a custom {@link se.jbee.inject.action.ActionExecutor} implementation.
 */
package se.jbee.inject.action;
