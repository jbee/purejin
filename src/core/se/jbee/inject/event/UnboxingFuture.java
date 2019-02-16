package se.jbee.inject.event;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static se.jbee.inject.event.EventException.getFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * When computing something asynchronously that itself returns a {@link Future}
 * the computation becomes a {@link Future} of the original {@link Future}. This
 * implementation wraps this so that type wise this boxing is unboxed again. The
 * computational unboxing will first occur when {@link #get()} or
 * {@link #get(long, TimeUnit)} is called.
 * 
 * @param <T> type of the plain {@link Future}s value type
 */
class UnboxingFuture<T> implements Future<T> {

	final Future<? extends Future<T>> boxed;
	
	UnboxingFuture(Future<? extends Future<T>> boxed) {
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
	public T get() throws EventException {
		return getFuture(getFuture(boxed));
	}

	@Override
	public T get(long timeout, TimeUnit unit) 
			throws InterruptedException, ExecutionException, TimeoutException {
		long waitMillis = unit.toMillis(timeout);
		long start = currentTimeMillis();
		return boxed.get(timeout, unit)
				.get(waitMillis - (currentTimeMillis() - start), MILLISECONDS);	
	}
	
}