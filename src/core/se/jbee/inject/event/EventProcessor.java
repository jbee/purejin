package se.jbee.inject.event;

public interface EventProcessor {

	<T> void register(Class<T> event, T listener);

	<T> T proxy(Class<T> event);

}
