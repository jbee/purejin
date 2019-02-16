package se.jbee.inject.event;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public final class EventException extends RuntimeException {

	public EventException(Exception cause) {
		super(cause);
	}

	@Override
	public synchronized Exception getCause() {
		return (Exception) super.getCause();
	}
	
	public static <T> T getFuture(Future<T> f) throws EventException {
		try {
			return f.get();
		} catch (ExecutionException e) {
			if (e.getCause() instanceof EventException) {
				throw (EventException)e.getCause();
			}
			throw new EventException(e);
		} catch (InterruptedException e) {
			throw new EventException(e);
		}
	}
}
