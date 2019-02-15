package se.jbee.inject.event;

public final class EventException extends RuntimeException {

	public EventException(Exception cause) {
		super(cause);
	}

	@Override
	public synchronized Exception getCause() {
		return (Exception) super.getCause();
	}
}
