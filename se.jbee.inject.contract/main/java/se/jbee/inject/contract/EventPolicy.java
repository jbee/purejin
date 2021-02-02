/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.contract;

import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.TimeoutException;
import java.util.function.BinaryOperator;

import static java.lang.Math.max;

/**
 * A {@link EventPolicy} controls how the {@link Event}s are processed by a
 * {@link EventProcessor}.
 *
 * {@link EventProcessor} are optimised for throughput. Therefore some fault
 * tolerance features make less sense. A bulkhead is about flow control and
 * deliberately introduces waiting which is not desirable for an
 * {@link EventProcessor}. A timeout or circuit breaker for individual handlers
 * are no good fit for a multi-dispatch system. Fault tolerance comes from
 * redundancy (multiple handlers) and eventually retrying.
 *
 * @since 8.1
 */
public final class EventPolicy implements Serializable {

	//TODO 2020-08-05: Add...
	// 1. How to Dispatch enum: MULTI, ROUND_ROBIN, ...
	// 2. What is success? one handled it, all handled it, n-m handled it (within what timeframe?)
	// 3. how to indicate failure? throw exception, return null, return default, ...

	public enum Flags {

		/* Multi-Dispatch Handling */
		/**
		 * Whether or not to use multi-dispatch for methods with return type
		 * {@link Void} or {@code void}.
		 *
		 * Default is {@code true} (do multi-dispatch to all handlers).
		 *
		 * Methods with other returns types are dispatched to one of the
		 * registered handlers switched by a round robin strategy.
		 */
		MULTI_DISPATCH,

		/**
		 * Whether or not to block and wait for multi-dispatch until all
		 * handlers complete the call.
		 *
		 * Default is {@code false} (no sync).
		 */
		MULTI_DISPATCH_SYNC,

		/**
		 * Whether or not to use multi-dispatch and aggregate the individual
		 * results.
		 *
		 * For return type {@link Boolean} or {@code boolean} aggregation is AND
		 * operator. For integers it is {@link Integer#sum(int, int)}. Other
		 * aggregation functions are picked up dynamically in case the last
		 * method parameter is of raw type {@link BinaryOperator}.
		 *
		 * Default is {@code false} (no aggregation).
		 */
		MULTI_DISPATCH_AGGREGATED,

		/* Exception Handling */
		/**
		 * Whether or not to return {@code null}, zero or {@code false} in case
		 * there is no handler for the event instead of throwing an
		 * {@link EventException}.
		 */
		RETURN_NO_HANDLER_AS_NULL
	}

	public static final EventPolicy DEFAULT = new EventPolicy(Integer.MAX_VALUE,
			Runtime.getRuntime().availableProcessors(), 0, "aggregator",
			EnumSet.of(Flags.MULTI_DISPATCH));

	/**
	 * The number of times an {@link Event} attempts again to be handled by each
	 * handler after it failed to be handled successfully.
	 */
	public final int maxRetries;

	/**
	 * The maximum number of threads that should be allowed to run *any* of the
	 * event interfaces methods concurrently.
	 *
	 * So any threading issue within any of the methods can be avoided by
	 * setting this to 1 which assures isolation across *all* methods of the
	 * event interface. That means if any thread calls any of the methods no
	 * other method will be called until the call is complete. This has the same
	 * effect as if all method were marked {@code synchronized}. However, this
	 * is only true as long as the user does not manually call the
	 * implementation methods as only the calls made by the
	 * {@link EventProcessor} will be coordinated.
	 */
	public final int maxConcurrency;

	/**
	 * The maximum number of milliseconds the event may be in the queue (before
	 * starting processing) that is still accepted and processed.
	 *
	 * If the TTL is exceeded before the processing is started the event will
	 * throw a {@link EventException} with a cause of a
	 * {@link TimeoutException}.
	 *
	 * A zero or negative TTL means there is no Time To Live and all events are
	 * processed no matter how long they wait in the queue.
	 */
	public final int ttl;

	/**
	 * The name used when trying to resolve an {@link BinaryOperator} aggregator
	 * function using the context.
	 */
	public final String aggregatorName;
	@SuppressWarnings("squid:S1319")
	private final EnumSet<Flags> flags;

	//TODO what is Success? dispatch to 1 of many in round robin, dispatch to all?, dispatch to x% of many?

	private EventPolicy(int maxAttempts, int maxConcurrency, int ttl, String aggregatorName,
			EnumSet<Flags> flags) {
		this.maxRetries = max(0, maxAttempts);
		this.maxConcurrency = max(1, maxConcurrency);
		this.ttl = ttl;
		this.aggregatorName = aggregatorName;
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

	public EventPolicy withTTL(int ttl) {
		return new EventPolicy(maxRetries, maxConcurrency, ttl, aggregatorName, flags);
	}

	public EventPolicy withMaxConcurrency(int n) {
		return new EventPolicy(maxRetries, n, ttl, aggregatorName, flags);
	}

	public EventPolicy withMaxRetries(int n) {
		return new EventPolicy(n, maxConcurrency, ttl, aggregatorName, flags);
	}

	public EventPolicy withAggregator(String name) {
		return new EventPolicy(maxRetries, maxConcurrency, ttl, name, flags);
	}

	public EventPolicy with(Flags flag) {
		EnumSet<Flags> merged = EnumSet.copyOf(this.flags);
		merged.add(flag);
		return new EventPolicy(maxRetries, maxConcurrency, ttl, aggregatorName, merged);
	}

	public EventPolicy with(Flags... flags) {
		EnumSet<Flags> fs = EnumSet.copyOf(this.flags);
		fs.addAll(Arrays.asList(flags));
		return new EventPolicy(maxRetries, maxConcurrency, ttl, aggregatorName, fs);
	}

	@Override
	public String toString() {
		return maxConcurrency + ":" + ttl + " " + flags;
	}

}
