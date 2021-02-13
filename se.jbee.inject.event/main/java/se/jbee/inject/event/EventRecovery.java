package se.jbee.inject.event;

@FunctionalInterface
public interface EventRecovery {

	interface Controller {

	}

	<T> void recover(Exception ex, Event<T> event, EventHandler<? super T> handler, Controller controller);
}
