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

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.lang.Cast.consumerTypeOf;
import static test.integration.Assertions.assertSameElements;
import static test.integration.Assertions.assertTrueWithin;

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

			// in this test we want the scheduler to call sync every 5ms
			configure(DiskScope.class) //
					.bind("syncTime", long.class).to(5L); //ms
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
		Env env = Bootstrap.DEFAULT_ENV.with(consumerTypeOf(DiskScope.DiskEntry.class), recorder);
		Injector context = Bootstrap.injector(env, TestFeatureDiskScopeSyncModule.class);

		assertEquals("Hello", context.resolve(String.class));
		assertTrueWithin(40L, recorded::size, greaterThanOrEqualTo(4));
		assertSameElements(recorded); // make sure all recorded entries are indeed the same
	}
}
