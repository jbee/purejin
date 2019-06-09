/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.event;

import static java.lang.System.currentTimeMillis;

import java.lang.reflect.Method;
import java.util.function.BinaryOperator;

import se.jbee.inject.Type;

/**
 * The message describing the handler interface method invocation as data.
 * 
 * @since 19.1
 * 
 * @param <E> type of the event handler interface
 * @param <T> return type of the {@link #target} method
 */
public final class Event<E, T> {

	/**
	 * The timestamp used to compute if a events TTL has expired or not.
	 */
	public final long created;
	public final Class<E> type;
	public final EventPreferences prefs;
	public final Type<T> result;
	public final Method target;
	public final Object[] args;
	/**
	 * The function used to aggregate multiple values if a computation is
	 * dispatched to more then one handler using
	 * {@link EventPreferences#isAggregatedMultiDispatch()}.
	 */
	public final BinaryOperator<T> aggregator;

	public Event(Class<E> event, EventPreferences prefs, Type<T> result,
			Method target, Object[] args, BinaryOperator<T> aggregator) {
		this.type = event;
		this.prefs = prefs;
		this.result = result;
		this.target = target;
		this.args = args;
		this.aggregator = aggregator;
		this.created = currentTimeMillis();
	}

	@Override
	public String toString() {
		return "[" + type.getSimpleName() + "]:" + target.getName();
	}

	public boolean isNonConcurrent() {
		return prefs.maxConcurrency == 1;
	}

	public boolean isExpired() {
		return prefs.ttl <= 0
			? false
			: currentTimeMillis() > created + prefs.ttl;
	}

	public boolean returnsVoid() {
		return result.rawType == void.class || result.rawType == Void.class;
	}
}