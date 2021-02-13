package se.jbee.inject.event;

public interface EventDispatcher {

	<T> void handle(Event<T> event, EventHandler<? super T> handler);
}
