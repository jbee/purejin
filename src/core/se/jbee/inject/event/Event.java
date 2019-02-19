package se.jbee.inject.event;

import static java.lang.System.currentTimeMillis;

import java.lang.reflect.Method;

import se.jbee.inject.Type;

/**
 * The message describing the handler interface method invocation as data.
 * 
 * @param <E> type of the event handler interface
 * @param <T> return type of the {@link #handler} method
 */
public final class Event<E, T> {

	/**
	 * The timestamp used to compute if a events TTL has expired or not.
	 */
	public final long created;
	public final Class<E> type;
	public final EventPreferences prefs;
	public final Type<T> result;
	public final Method handler;
	public final Object[] args;

	public Event(Class<E> event, EventPreferences prefs, Type<T> result, 
			Method handler, Object[] args) {
		this.type = event;
		this.prefs = prefs;
		this.result = result;
		this.handler = handler;
		this.args = args;
		this.created = currentTimeMillis();
	}
	
	@Override
	public String toString() {
		return "[" + type.getSimpleName() + "]:" + handler.getName();
	}

	public boolean isNonConcurrent() {
		return prefs.maxConcurrentUsage == 1;
	}
	
	public boolean isOutdated() {
		return prefs.ttl <= 0 
				? false 
				: currentTimeMillis() > created + prefs.ttl;
	}
	
	public boolean returnsVoid() {
		return result.rawType == void.class || result.rawType == Void.class;
	}
	
	public boolean returnsBoolean() {
		return result.rawType == boolean.class || result.rawType == Boolean.class;
	}
}