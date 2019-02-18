package se.jbee.inject.event;

import static java.lang.Math.max;

import java.util.concurrent.TimeoutException;

/**
 * Properties that control how the {@link Event}s are processed by a
 * {@link EventProcessor}.
 */
public final class EventProperties {
	
	public static final EventProperties DEFAULT = new EventProperties(
			Runtime.getRuntime().availableProcessors(), 0, 
			false, true, 
			false, false);
	
	/**
	 * The maximum number of threads that should be allowed to run *any* of the
	 * event interfaces methods concurrently.
	 * 
	 * So any threading issue within any of the methods can be avoided by setting
	 * this to 1 which assures isolation across *all* methods of the event
	 * interface. That means if any thread calls any of the methods no other method
	 * will be called until the call is complete.
	 */
	public final int maxConcurrentUsage;
	
	/**
	 * The maximum number of milliseconds the event may be in the queue (before
	 * starting processing) that is still accepted and processed.
	 * 
	 * If the ttl is exceeded before the processing is started the event will throw
	 * a {@link EventException} with a cause of a {@link TimeoutException}.
	 * 
	 * A zero or negative ttl means there is no Time To Live and all events are
	 * processed no matter how long they wait in the queue.
	 */
	public final int ttl;
	
	/**
	 * Whether or not to synchronise after a multi-dispatch so that the call to the
	 * handler method completes when the dispatch is done. Default should be
	 * {@code false}.
	 */
	public final boolean synchroniseVoids;
	
	/**
	 * Whether or not to use multi-dispatch for methods with return type
	 * {@link Void} or {@code void}. Default should be {@code true}.
	 */
	public final boolean multiDispatchVoids;
	
	/**
	 * Whether or not to use multi-dispatch for methods with return type
	 * {@link Boolean} or {@code boolean}. Default should be {@code false}.
	 */
	public final boolean multiDispatchBooleans;
	
	/**
	 * Whether or not to return {@code null} or zero or {@code false} in case there
	 * is no handler for the event instead of throwing an {@link EventException}.
	 */
	public final boolean noHandlerAsNullZeroFalse;
	
	public EventProperties(int maxConcurrentUsage, int ttl, 
			boolean synchroniseVoids, boolean multiDispatchVoids, 
			boolean multiDispatchBooleans, 
			boolean noHandlerAsNullZeroFalse) {
		this.maxConcurrentUsage = max(1, maxConcurrentUsage);
		this.ttl = ttl;
		this.synchroniseVoids = synchroniseVoids;
		this.multiDispatchVoids = multiDispatchVoids;
		this.multiDispatchBooleans = multiDispatchBooleans;
		this.noHandlerAsNullZeroFalse = noHandlerAsNullZeroFalse;
	}
	
	public EventProperties withTTL(int ttl) {
		return new EventProperties(maxConcurrentUsage, ttl, 
				synchroniseVoids, multiDispatchVoids, 
				multiDispatchBooleans, noHandlerAsNullZeroFalse);
	}
	
	public EventProperties withMaxConcurrentUsage(int n) {
		return new EventProperties(n, ttl, 
				synchroniseVoids, multiDispatchVoids, 
				multiDispatchBooleans, noHandlerAsNullZeroFalse);
	}
	
}