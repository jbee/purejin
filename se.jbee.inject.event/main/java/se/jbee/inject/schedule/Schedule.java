package se.jbee.inject.schedule;

import se.jbee.inject.Injector;
import se.jbee.inject.lang.Type;

import java.lang.reflect.Method;

public final class Schedule {

	@FunctionalInterface
	public interface ScheduleFactory {

		Schedule create(Object instance, Type<?> as, Method scheduled, Injector context);

	}

	public final Object instance;
	public final Type<?> as;
	public final Method scheduled;
	public final long intervalMillis;
	public final long initialDelayMillis;

	public Schedule(Object instance, Type<?> as, Method scheduled,
			long intervalMillis, long initialDelayMillis) {
		this.instance = instance;
		this.as = as;
		this.scheduled = scheduled;
		this.intervalMillis = intervalMillis;
		this.initialDelayMillis = initialDelayMillis;
	}
}
