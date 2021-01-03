package se.jbee.inject;

/**
 * A {@link DisconnectException} is thrown by {@link java.lang.reflect.Method}s
 * (that assume to be called as a consequence of being connected by a {@code
 * Connector}) in case they want to undo the connection and should no longer be
 * called.
 */
public class DisconnectException extends RuntimeException {

	public DisconnectException(String msg, Throwable cause) {
		super(msg, cause);
	}
	public DisconnectException(String msg) {
		super(msg);
	}
}
