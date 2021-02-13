package test.integration.event;

import se.jbee.inject.schedule.SchedulerModule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class RecordingScheduledExecutor
		implements SchedulerModule.ScheduledExecutor {

	static final class Job {
		final Runnable task;
		final long initialDelay;
		final long period;
		final TimeUnit unit;

		Job(Runnable task, long initialDelay, long period,
				TimeUnit unit) {
			this.task = task;
			this.initialDelay = initialDelay;
			this.period = period;
			this.unit = unit;
		}
	}

	final List<Job> recorded = new ArrayList<>();

	@Override
	public Future<?> executeInSchedule(Runnable task, long initialDelay,
			long period, TimeUnit unit) {
		Job job = new Job(task, initialDelay, period, unit);
		recorded.add(job);
		return CompletableFuture.completedFuture(job);
	}

	Job lastRecorded() {
		return recorded.isEmpty() ? null : recorded.get(recorded.size() - 1);
	}
}
