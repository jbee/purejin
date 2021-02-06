package se.jbee.inject.schedule;

import se.jbee.inject.Injector;
import se.jbee.inject.lang.Type;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;

public final class Schedule {

	@FunctionalInterface
	public interface ScheduleFactory {

		Schedule create(Object instance, Type<?> as, Method scheduled, Injector context);

	}

	public final Object instance;
	public final Type<?> as;
	public final Method scheduled;
	public final Duration interval;
	public final LocalDateTime firstRun;

	public Schedule(Object instance, Type<?> as, Method scheduled,
			Duration interval, LocalDateTime firstRun) {
		this.instance = instance;
		this.as = as;
		this.scheduled = scheduled;
		this.interval = interval;
		this.firstRun = firstRun;
	}

	public Duration delayNow() {
		Duration delay = Duration.between(LocalDateTime.now(), firstRun);
		return delay.isNegative() ? Duration.ZERO : delay;
	}
}
