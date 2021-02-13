package se.jbee.inject.schedule;

import se.jbee.inject.Injector;
import se.jbee.inject.event.EventTarget;
import se.jbee.lang.Type;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;

public final class Schedule {

	public static class Run {

		public final EventTarget target;

		public Run(EventTarget target) {
			this.target = target;
		}
	}

	@FunctionalInterface
	public interface ScheduleFactory {

		Schedule create(Object instance, Type<?> as, Method scheduled, Injector context);

	}

	public final Object instance;
	public final Type<?> as;
	public final Method scheduled;
	public final Duration interval;
	public final LocalDateTime firstRun;
	public final int cancelAfterConsecutiveFailedRuns;

	//TODO maybe rather have callbacks like: onRunFailed(Exception ex, ExecutionControl control); where control allows to e.g. cancel the execution and such things

	public Schedule(Object instance, Type<?> as, Method scheduled,
			Duration interval, LocalDateTime firstRun, int cancelAfterConsecutiveFailedRuns) {
		this.instance = instance;
		this.as = as;
		this.scheduled = scheduled;
		this.interval = interval;
		this.firstRun = firstRun;
		this.cancelAfterConsecutiveFailedRuns = cancelAfterConsecutiveFailedRuns;
	}

	public Duration delayNow() {
		Duration delay = Duration.between(LocalDateTime.now(), firstRun);
		return delay.isNegative() ? Duration.ZERO : delay;
	}

	public boolean cancelAfterFailedRuns() {
		return cancelAfterConsecutiveFailedRuns > 0;
	}

	@Override
	public String toString() {
		return String.format("%s[%s] every %s from %s",
				getClass().getSimpleName(), scheduled, interval, firstRun);
	}
}
