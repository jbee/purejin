/*
 *  Copyright (c) 2012-2019, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.event;

/**
 * A function to derive event handling preferences from the event type.
 * 
 * This should allow users to use their own mechanism and logic. For example the
 * use of user annotations to describe isolation and such preferences that are
 * check by the users implementation of this interface to convert them to a
 * universal description {@link EventPreferences}.
 * 
 * To substitute the default behaviour (which used
 * {@link EventPreferences#DEFAULT}) simply bind {@link EventMirror} to a
 * custom supplier having the behaviour you want.
 * 
 * @since 19.1
 */
@FunctionalInterface
public interface EventMirror {

	/**
	 * Extracts the {@link EventPreferences} to use for the given event type.
	 * 
	 * The implementation uses reflection to look at signatures and/or
	 * annotations to derive the {@link EventPreferences} which are used by the
	 * {@link EventProcessor} to control the processing of events of the given
	 * type.
	 * 
	 * @param event the type of the event (the event interface)
	 * @return the {@link EventPreferences} to use.
	 */
	EventPreferences reflect(Class<?> event);
}
