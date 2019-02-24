package se.jbee.inject.event;

import static java.lang.Math.max;

import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.TimeoutException;

/**
 * Properties that control how the {@link Event}s are processed by a
 * {@link EventProcessor}.
 */
public final class EventPreferences implements Serializable {
	
	public enum Flags {
		
		/* Multi-Dispatch Handling */
		/**
		 * Whether or not to use multi-dispatch for methods with return type
		 * {@link Void} or {@code void}. Default should be {@code true}.
		 */
		MULTI_DISPATCH,

		/**
		 * Whether or not to synchronise after a multi-dispatch so that the call to the
		 * handler method completes when the dispatch is done. Default should be
		 * {@code false}.
		 */
		MULTI_DISPATCH_SYNC,
		
		/**
		 * Whether or not to use multi-dispatch for methods with return type
		 * {@link Boolean} or {@code boolean} and combine their result with OR operator.
		 * Default should be {@code false}.
		 */
		MULTI_DISPATCH_AGGREGATED,
		
		/* Exception Handling */
		/**
		 * Whether or not to return {@code null} or zero or {@code false} in case there
		 * is no handler for the event instead of throwing an {@link EventException}.
		 */
		RETURN_NO_HANDLER_AS_NULL;
	}
	
	public static final EventPreferences DEFAULT = new EventPreferences(
			Runtime.getRuntime().availableProcessors(), 0, 
			EnumSet.of(Flags.MULTI_DISPATCH));
	
	/**
	 * The maximum number of threads that should be allowed to run *any* of the
	 * event interfaces methods concurrently.
	 * 
	 * So any threading issue within any of the methods can be avoided by setting
	 * this to 1 which assures isolation across *all* methods of the event
	 * interface. That means if any thread calls any of the methods no other method
	 * will be called until the call is complete. The effect is as if all method
	 * would have been marked {@code synchronized}. However, this is only true as
	 * long as the user does not manually call the implementation methods as only
	 * the calls made by the {@link EventProcessor} will be coordinated.
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
	
	private final EnumSet<Flags> flags;
	
	public EventPreferences(int maxConcurrentUsage, int ttl, EnumSet<Flags> flags) {
		this.maxConcurrentUsage = max(1, maxConcurrentUsage);
		this.ttl = ttl;
		this.flags = flags;
	}
	
	public boolean isSyncMultiDispatch() {
		return flags.contains(Flags.MULTI_DISPATCH_SYNC);
	}
	
	public boolean isMultiDispatch() {
		return flags.contains(Flags.MULTI_DISPATCH);
	}
	
	public boolean isAggregatedMultiDispatch() {
		return flags.contains(Flags.MULTI_DISPATCH_AGGREGATED);
	}
	
	public boolean isReturnNoHandlerAsNull() {
		return flags.contains(Flags.RETURN_NO_HANDLER_AS_NULL);
	}
	
	public EventPreferences withTTL(int ttl) {
		return new EventPreferences(maxConcurrentUsage, ttl, flags);
	}
	
	public EventPreferences withMaxConcurrentUsage(int n) {
		return new EventPreferences(n, ttl, flags);
	}
	
	public EventPreferences with(Flags flag) {
		EnumSet<Flags> flags = EnumSet.copyOf(this.flags);
		flags.add(flag);
		return new EventPreferences(maxConcurrentUsage, ttl, flags);
	}
	
	public EventPreferences with(Flags...flags) {
		EnumSet<Flags> fs = EnumSet.copyOf(this.flags);
		fs.addAll(Arrays.asList(flags));
		return new EventPreferences(maxConcurrentUsage, ttl, fs);
	}
	
	@Override
	public String toString() {
		return maxConcurrentUsage + ":"+ttl+" "+flags;
	}
	
}