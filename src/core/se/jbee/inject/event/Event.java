package se.jbee.inject.event;

import static java.lang.System.currentTimeMillis;

import java.lang.reflect.Method;

import se.jbee.inject.Type;

public final class Event<E, T> {

	/**
	 * The timestamp used to compute if a events TTL has expired or not.
	 */
	public final long created;
	public final Class<E> type;
	public final EventProperties properties;
	public final Type<T> result;
	public final Method handler;
	public final Object[] args;

	public Event(Class<E> event, EventProperties properties, Type<T> result, 
			Method handler, Object[] args) {
		this.type = event;
		this.properties = properties;
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
		return properties.maxConcurrentUsage == 1;
	}
	
	public boolean isOutdated() {
		return properties.ttl <= 0 
				? false 
				: currentTimeMillis() > created + properties.ttl;
	}
	
	public boolean returnsVoid() {
		return result.rawType == void.class || result.rawType == Void.class;
	}
	
	public boolean returnsBoolean() {
		return result.rawType == boolean.class || result.rawType == Boolean.class;
	}
}