package test.integration.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.Scope;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.Installs;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.disk.DiskScope;
import se.jbee.inject.disk.DiskScopeModule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A test that verifies that objects in disk scope are restored from disk.
 *
 * @see TestFeatureDiskScopeSync
 */
@Disabled("because makes `bach test` stall")
class TestFeatureDiskScopePersistence {

	static final File dir = new File("target/scope/test");
	static final Name myDisk = Scope.disk(dir);

	@Installs(bundles = DiskScopeModule.class)
	private static final class TestFeatureDiskScopePersistenceModule
			extends BinderModule {

		@Override
		protected void declare() {
			AtomicInteger counter = new AtomicInteger();
			per(myDisk).bind(AtomicInteger.class).toProvider(() -> {
				// increment each time this provider is asked so we know
				counter.incrementAndGet();
				return counter;
			});
		}
	}

	@BeforeEach
	void cleanDir() throws IOException {
		if (dir.exists())
			Files.newDirectoryStream(dir.toPath())
					.forEach(file -> file.toFile().delete());
		Files.deleteIfExists(dir.toPath());
	}

	@Test
	void diskScopePreservesStateOnDisk() {
		Injector injector = Bootstrap.injector(
				TestFeatureDiskScopePersistenceModule.class);
		AtomicInteger actualCounter = injector.resolve(AtomicInteger.class);
		assertEquals(1, actualCounter.intValue());
		actualCounter = injector.resolve(AtomicInteger.class);
		assertEquals(1, actualCounter.intValue());

		DiskScope disk = (DiskScope) injector.resolve(myDisk, Scope.class);
		assertNotNull(disk);
		actualCounter.set(5); // somebody changed the inner state of the disk scoped object

		// simulated restart
		disk.close();
		assertNotNull(Bootstrap.injector(TestFeatureDiskScopePersistenceModule.class));

		assertEquals(5, actualCounter.intValue());
	}
}
