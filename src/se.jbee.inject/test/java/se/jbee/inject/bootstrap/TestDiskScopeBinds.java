package se.jbee.inject.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.Scope;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.scope.DiskScope;

public class TestDiskScopeBinds {

	static final File dir = new File("target/scope/test");
	static final Name myDisk = Scope.disk(dir);

	private static final class DiskScopeBindsModule extends BinderModule {

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

	@Before
	public void cleanDir() throws IOException {
		if (dir.exists())
			for (File file : dir.listFiles())
				if (!file.isDirectory())
					file.delete();
		Files.deleteIfExists(dir.toPath());
	}

	@Test
	@Ignore("Underlying file handle is never closed -- stalls on Windows.")
	public void diskScopePreservesStateOnDisk() {
		Injector injector = Bootstrap.injector(DiskScopeBindsModule.class);
		AtomicInteger actualCounter = injector.resolve(AtomicInteger.class);
		assertEquals(1, actualCounter.intValue());
		actualCounter = injector.resolve(AtomicInteger.class);
		assertEquals(1, actualCounter.intValue());

		DiskScope disk = (DiskScope) injector.resolve(myDisk, Scope.class);
		assertNotNull(disk);
		actualCounter.set(5); // somebody changed the inner state of the disk object

		// simulated restart
		disk.close();
		injector = Bootstrap.injector(DiskScopeBindsModule.class);

		assertEquals(5, actualCounter.intValue());
	}
}
