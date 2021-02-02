/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.contract;

/**
 * A function to derive event handling {@link EventPolicy} from the event type.
 *
 * This should allow users to use their own mechanism and logic. For example the
 * use of user annotations to describe isolation and such preferences that are
 * checked by the users implementation of this interface to convert them to a
 * universal description: {@link EventPolicy}.
 *
 * To substitute the default behaviour (which used {@link EventPolicy#DEFAULT})
 * simply bind {@link PolicyProvider} to a custom supplier having the behaviour you
 * want.
 *
 * @since 8.1
 */
@FunctionalInterface
public interface PolicyProvider {

	/**
	 * Extracts the {@link EventPolicy} to use for the given event type.
	 *
	 * The implementation uses reflection to look at signatures and/or
	 * annotations to derive the {@link EventPolicy} which are used by the
	 * {@link EventProcessor} to control the processing of events of the given
	 * type.
	 *
	 * @param handlerType the type of the event (the event handler interface)
	 * @return the {@link EventPolicy} to use.
	 */
	EventPolicy reflect(Class<?> handlerType);
}
