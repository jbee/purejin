/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.contract;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.*;

/**
 * Is thrown by the event interface proxy for calls that should return a value
 * to the caller.
 *
 * It {@link #getCause()} indicates what went wrong.
 *
 * @since 8.1
 */
public final class EventException extends RuntimeException {

	public static <T> T unwrapGet(Event<?, T> event, Future<T> f)
			throws Exception {
		return unwrap(event, f::get);
	}

	/**
	 *
	 * @param event the event processed
	 * @param func the function that may throw an {@link Exception}
	 * @return the functions value
	 * @throws Exception This is either the exception thrown by the handler
	 *             method or an {@link EventException} in case the problem was
	 *             not within the handler method but a problem of processing the
	 *             event in the {@link EventProcessor}.
	 */
	public static <T> T unwrap(Event<?, ? extends T> event, Callable<T> func)
			throws Exception {
		try {
			return func.call();
		} catch (ExecutionException e) {
			if (e.getCause() instanceof EventException) {
				EventException ee = (EventException) e.getCause();
				if (ee.isCausedByHandlerException())
					throw (Exception) ((InvocationTargetException) ee.getCause()).getTargetException();
				if (ee.isCausedByNoHandler()
					&& event.policy.isReturnNoHandlerAsNull())
					return null;
				if (ee.isCausedByTimeout())
					for (Class<?> et : event.target.getExceptionTypes())
						if (et.isAssignableFrom(TimeoutException.class))
							throw ee.getCause();
				throw ee;
			}
			throw new EventException(event, e);
		} catch (InterruptedException | EventException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new EventException(event, ex);
		}
	}

	/**
	 * Might be null in case of {@link InterruptedException}.
	 */
	public final transient Event<?, ?> event;

	public EventException(Event<?, ?> event, Exception cause) {
		super(cause);
		this.event = event;
	}

	@Override
	public synchronized Exception getCause() {
		return (Exception) super.getCause();
	}

	/**
	 * @return true if the cause of the exception was that no handler
	 *         implementation was known/available to process the event.
	 */
	public boolean isCausedByNoHandler() {
		return getCause() == null;
	}

	/**
	 * @return true if the cause of the exception was {@link EventProcessor}
	 *         rejecting to process the event.
	 */
	public boolean isCausedByRejection() {
		return getCause() instanceof RejectedExecutionException;
	}

	/**
	 * @return true if the event should be processed after its
	 *         {@link EventPolicy#ttl} period already has passed.
	 */
	public boolean isCausedByTimeout() {
		return getCause() instanceof TimeoutException;
	}

	/**
	 * @return true if the event was processed but the handler method used throw
	 *         an exception during execution.
	 */
	public boolean isCausedByHandlerException() {
		Exception cause = getCause();
		return cause instanceof InvocationTargetException;
	}
}
