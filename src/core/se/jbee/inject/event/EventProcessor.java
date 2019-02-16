package se.jbee.inject.event;

import java.util.concurrent.Future;

public interface EventProcessor extends AutoCloseable {

	/**
	 * This will cause the calling thread to wait until there is a implementation
	 * for the given event.
	 *
	 * Instead of support blocking in case no implementation is known for a event
	 * the API allows to call this method explicitly to wait until a implementation
	 * is registered and be notified. This allows to build any kind of custom
	 * blocking wait logic around the {@link EventProcessor}.
	 * 
	 * @param event type of event handler to wait for.
	 */
	<E> void await(Class<E> event) throws InterruptedException;
	
	<E> void register(Class<E> event, E handler);

	<E> void deregister(Class<E> event, E handler);
	
	<E> E getProxy(Class<E> event);

	<E, T> void dispatch(Event<E, T> event);
	
	<E, T> T compute(Event<E, T> event) throws EventException;
	
	<E, T extends Future<V>, V> Future<V> computeEventually(Event<E, T> event) throws EventException;
	
	@Override
	void close();
}
