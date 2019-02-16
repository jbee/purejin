package se.jbee.inject.event;

/**
 * A function to derive event handling properties from the event type.
 * 
 * This should allow users to use their own mechanism and logic. For example the
 * use of user annotations to describe isolation and such properties that are
 * check by the users implementation of this interface to convert them to a
 * universal description {@link EventProperties}.
 */
@FunctionalInterface
public interface EventReflector {
	
	EventProperties reflect(Class<?> event);
}
