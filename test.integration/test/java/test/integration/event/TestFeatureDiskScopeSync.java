package test.integration.event;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.Scope;
import se.jbee.inject.binder.BinderModuleWith;
import se.jbee.inject.binder.Installs;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.disk.DiskScope;
import se.jbee.inject.disk.DiskScopeModule;
import se.jbee.inject.schedule.SchedulerModule;
import test.integration.event.RecordingScheduledExecutor.Job;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.jbee.lang.Cast.consumerTypeOf;
import static se.jbee.junit.assertion.Assertions.assertAllSame;

/**
 * A test to verify that the {@link se.jbee.inject.schedule.Scheduled}
 * annotation on the {@link DiskScope} is picked up and called in intervals.
 * Secondly this also tests indirectly that the interval length is configurable
 * as a particular short interval is set as part of the test setup.
 *
 * @see TestFeatureDiskScopePersistence
 */
class TestFeatureDiskScopeSync {

	static final File dir = new File("target/scope/in-memory");
	static final Name inMemoryDisk = Scope.disk(dir);

	@Installs(bundles = DiskScopeModule.class)
	private static final class TestFeatureDiskScopeSyncModule extends
			BinderModuleWith<Consumer<DiskScope.DiskEntry>> {

		@Override
		protected void declare(Consumer<DiskScope.DiskEntry> recorder) {
			per(inMemoryDisk).bind(String.class).toScoped("Hello");

			bind(consumerTypeOf(DiskScope.DiskEntry.class)).to(recorder);

			// in this test we want the scheduler to call sync every 20ms
			configure(DiskScope.class) //
					.bind("syncTime", long.class).to(20L); //ms

			// but we also "stub" the executor so we can simulate the calls
			// and check we get instructed correctly
			bind(SchedulerModule.ScheduledExecutor.class)
					.to(env().property(SchedulerModule.ScheduledExecutor.class));
		}
	}

	/**
	 * When disk is asked to store the entry at least 4 times this means
	 * that in general scheduling has picked up the annotated method as well
	 * as that the configuration change did have an effect on the interval.
	 */
	@Test
	void schedulerCallsSyncInConfiguredInterval() {
		List<DiskScope.DiskEntry> recorded = new CopyOnWriteArrayList<>();
		Consumer<DiskScope.DiskEntry> recorder = recorded::add;
		RecordingScheduledExecutor executor = new RecordingScheduledExecutor();
		Env env = Bootstrap.DEFAULT_ENV
				.with(consumerTypeOf(DiskScope.DiskEntry.class), recorder)
				.with(SchedulerModule.ScheduledExecutor.class, executor);
		Injector context = Bootstrap.injector(env, TestFeatureDiskScopeSyncModule.class);

		assertEquals("Hello", context.resolve(String.class));
		// after we now have created the scheduled "bean" and by that the scheduler
		// the executor should have gotten a job
		assertEquals(1, executor.recorded.size());
		// verify the details of that job
		Job last = executor.lastRecorded();
		assertNotNull(last);
		assertNotNull(last.task);
		assertEquals(20L, last.unit.toMillis(last.period));
		assertEquals(0L, last.initialDelay);
		// now we simulate the scheduler work
		assertEquals(1, recorded.size(), "loading does not update to disk");
		last.task.run();
		assertEquals(2, recorded.size());
		last.task.run();
		assertEquals(3, recorded.size());
		last.task.run();
		assertEquals(4, recorded.size());
		assertAllSame(recorded); // make sure all recorded entries are indeed the same
	}

}
