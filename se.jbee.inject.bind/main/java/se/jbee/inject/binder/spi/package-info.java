/**
 * This package contains a number of interfaces that are used to build fluent
 * APIs to make bindings.
 * <p>
 * Each of the interfaces is a {@link java.lang.FunctionalInterface} that based
 * on on essential abstract method adds further {@code default} methods that
 * operate on the essential method. In most cases they provide an alternative
 * parameter list to allow use of different arguments in a convenient way. This
 * convenience "layer" can be added to API implementation by implementing the
 * interface.
 * <p>
 * All interfaces have a type parameter for the binder type that is returned by
 * all of their methods so that implementations can return their specific binder
 * types.
 */
package se.jbee.inject.binder.spi;
