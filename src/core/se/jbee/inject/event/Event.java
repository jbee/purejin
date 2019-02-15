package se.jbee.inject.event;

import static java.lang.System.currentTimeMillis;

import java.lang.reflect.Method;

import se.jbee.inject.Type;

public final class Event<E, T> {

	public final long created;
	//TODO ttl - this is a timeout to be started - that means if the event is picked up too late it is not processed but cancelled throwing a TimeoutException 
	public final Class<E> type;
	public final Type<T> result;
	public final Method handler;
	public final Object[] args;

	public Event(Class<E> event, Type<T> result, Method handler, Object[] args) {
		this.type = event;
		this.result = result;
		this.handler = handler;
		this.args = args;
		this.created = currentTimeMillis();
	}
	
	@Override
	public String toString() {
		return "[" + type.getSimpleName() + "]:" + handler.getName();
	}
}