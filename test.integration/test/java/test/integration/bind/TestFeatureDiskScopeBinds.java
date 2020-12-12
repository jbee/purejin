package test.integration.bind;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.Scope;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.scope.DiskScope;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestFeatureDiskScopeBinds {

	static final File dir = new File("target/scope/test");
	static final Name myDisk = Scope.disk(dir);

	private static final class TestFeatureDiskScopeBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			AtomicInteger counter = new AtomicInteger();
			per(myDisk).bind(AtomicInteger.class).to(() -> {
				// increment each time this provider is asked so we know
				counter.incrementAndGet();
				return counter;
			});
		}
	}

	@BeforeEach
	void cleanDir() throws IOException {
		if (dir.exists())
			for (File file : dir.listFiles())
				if (!file.isDirectory())
					file.delete();
		Files.deleteIfExists(dir.toPath());
	}

	@Test
	@Disabled("because...windows file locks")
	void diskScopePreservesStateOnDisk() {
		Injector injector = Bootstrap.injector(TestFeatureDiskScopeBindsModule.class);
		AtomicInteger actualCounter = injector.resolve(AtomicInteger.class);
		assertEquals(1, actualCounter.intValue());
		actualCounter = injector.resolve(AtomicInteger.class);
		assertEquals(1, actualCounter.intValue());

		DiskScope disk = (DiskScope) injector.resolve(myDisk, Scope.class);
		assertNotNull(disk);
		actualCounter.set(5); // somebody changed the inner state of the disk object

		// simulated restart
		disk.close();
		assertNotNull(Bootstrap.injector(TestFeatureDiskScopeBindsModule.class));

		assertEquals(5, actualCounter.intValue());
	}
}
