package se.jbee.inject.event;

import static java.lang.Math.max;

/**
 * A function to derive event handling properties from event type hand specific
 * handler implementation.
 * 
 * This should allow users to use their own mechanism and logic. For example the
 * use of user annotations to describe isolation and such properties that are
 * check by the users implementation of this interface to convert them to a
 * universal description {@link EventHandlerProperties}.
 */
@FunctionalInterface
public interface EventHandlerReflector {
	
	<E> EventHandlerProperties getProperties(Class<E> event, E handler);
	
	class EventHandlerProperties {
		
		/**
		 * The maximum number of threads that should be allowed to run *any* of the
		 * event interfaces methods concurrently.
		 * 
		 * So any threading issue within any of the methods can be avoided by setting
		 * this to 1 which assures isolation across *all* methods of the event
		 * interface. That means if any thread calls any of the methods no other method
		 * will be called until the call is complete.
		 */
		public final int maxConcurrentUsage;
		
		public EventHandlerProperties(int maxConcurrentUsage) {
			this.maxConcurrentUsage = max(1, maxConcurrentUsage);
		}
		
	}
}
