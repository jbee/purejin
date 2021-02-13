/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.contract;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * When computing something asynchronously that itself returns a {@link Future}
 * the computation becomes a {@link Future} of the original {@link Future}. This
 * implementation wraps this so that type wise this boxing is unboxed again. The
 * computational unboxing will first occur when {@link #get()} or
 * {@link #get(long, TimeUnit)} is called.
 *
 * @since 8.1
 *
 * @param <T> type of the plain {@link Future}s value type
 */
public final class UnboxingFuture<T> implements Future<T> {

	private final Event<?, ? extends Future<T>> event;
	private final Future<? extends Future<T>> boxed;

	UnboxingFuture(Event<?, ? extends Future<T>> event,
			Future<? extends Future<T>> boxed) {
		this.event = event;
		this.boxed = boxed;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return boxed.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return boxed.isCancelled();
	}

	@Override
	public boolean isDone() {
		return boxed.isDone();
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		Future<T> unboxed;
		try {
			unboxed = EventException.unwrap(event, boxed::get);
		} catch (ExecutionException | InterruptedException e) {
			throw e;
		} catch (EventException e) {
			if (e.isCausedByHandlerException())
				throw (ExecutionException) e.getCause();
			throw e;
		} catch (Exception e) {
			throw new ExecutionException(e);
		}
		return unboxed.get();
	}

	@Override
	@SuppressWarnings("squid:S1941")
	public T get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		long start = currentTimeMillis();
		Future<T> unboxed;
		try {
			unboxed = EventException.unwrap(event,
					() -> boxed.get(timeout, unit));
		} catch (ExecutionException | InterruptedException
				| TimeoutException e) {
			throw e;
		} catch (EventException e) {
			if (e.isCausedByHandlerException())
				throw (ExecutionException) e.getCause();
			if (e.isCausedByTimeout())
				throw (TimeoutException) e.getCause();
			throw e;
		} catch (Exception e) {
			throw new ExecutionException(e);
		}
		long waitMillis = unit.toMillis(timeout);
		long left = waitMillis - (currentTimeMillis() - start);
		if (left <= 0) {
			unboxed.cancel(false);
			throw new TimeoutException();
		}
		return unboxed.get(left, MILLISECONDS);
	}

}
