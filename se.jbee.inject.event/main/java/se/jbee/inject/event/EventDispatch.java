package se.jbee.inject.event;

import java.util.Map;

/**
 * The {@link EventDispatch} calls zero, one or more {@link
 * EventHandler}s that according to their strategy should handle the dispatched
 * {@link Event}.
 *
 * @param <T> type of the raw event dispatched
 */
@FunctionalInterface
public interface EventDispatch<T> {

	/**
	 * Selects the {@link EventHandler}(s) to use to {@link
	 * EventHandler#handle(Event)} the event.
	 */
	void dispatch(Event<T> event, Map<EventTarget, EventHandler<? super T>> handlers,
			EventDispatcher dispatcher);

	//NOTE: fallback is part of dispatch
}
