package se.jbee.inject.event;

import se.jbee.lang.Type;

import java.util.function.Consumer;

/**
 * An {@link EventTrigger} is an independent source of a particular type of
 * event.
 * <p>
 * When an event type has known receivers the corresponding {@link EventTrigger}
 * is resolved and {@link #activate(Consumer)} is called passing in a callback
 * to the dispatch which should be used by the trigger in case it wants to
 * trigger an event of its type in the future.
 *
 * @param <T> type of the event triggered by the implementation
 */
@FunctionalInterface
public interface EventTrigger<T> {

	static <T> Type<EventTrigger<T>> eventTriggerTypeOf(Class<T> event) {
		return eventTriggerTypeOf(Type.raw(event));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <T> Type<EventTrigger<T>> eventTriggerTypeOf(Type<T> event) {
		return (Type) Type.raw(EventTrigger.class).parameterized(event);
	}

	/**
	 * A receiver for the event type has been encountered, the trigger is asked
	 * from now on to trigger event by passing the event object to the provided
	 * dispatcher callback.
	 *
	 * @param dispatcher should be called to trigger events
	 */
	void activate(Consumer<T> dispatcher);
}
