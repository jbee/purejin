package se.jbee.inject.event;

public final class Event<T> {

	public final T type;

	public Event(T type) {
		this.type = type;
	}

	//TODO add parent Event and a event loop detection
	// similar to dependencies each time an event handler triggers new events those become
	// children of that event just that in case of events the children refer to their parent
	// initial event (from a trigger) do not have a parent event
}
