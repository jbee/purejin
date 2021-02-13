package se.jbee.junit.assertion;

import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;

import java.util.concurrent.*;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * This is a small and simple utility to assert things about asynchronously
 * changing conditions.
 * <p>
 * It is inspired by the {@code awaitility} library but works on top of {@code
 * junit5} and only has very essential functionality.
 */
final class AssertAwait {

	private static final ExecutorService executor = Executors.newWorkStealingPool();

	private AssertAwait() {
		throw new UnsupportedOperationException("util");
	}

	public static void assertAwait(Await await, Executable test) {
		assertAwait(await, () -> {
			try {
				test.execute();
				return null;
			} catch (AssertionFailedError e) {
				return e;
			} catch (Throwable t) {
				return fail("await test threw unexpected exception: ", t);
			}
		});
	}

	public static void assertAwait(Await await, Callable<AssertionFailedError> test) {
		long maxWaitMillis = await.maxWaitTime.toMillis();
		long minWaitMillis = await.minWaitTime.toMillis();
		long pollIntervalMillis = await.pollInterval().toMillis();
		long waitTimeLeftMillis = maxWaitMillis;
		long waitStarted = currentTimeMillis();
		AssertionFailedError lastError = null;
		Future<AssertionFailedError> error = null;
		while (waitTimeLeftMillis > 0) {
			try {
				error = executor.submit(test);
				if (executor.isShutdown() || executor.isTerminated())
					fail("await execution was canceled.");
				try {
					lastError = error.get(waitTimeLeftMillis,
							TimeUnit.MILLISECONDS);
					long timePassed = currentTimeMillis() - waitStarted;
					if (lastError == null) {
						if (timePassed < minWaitMillis)
							fail(format(
									"await condition met before minimum time %s had passed",
									await.minWaitTime));
						return; // success
					}
					sleepUninterruptible(pollIntervalMillis - timePassed);
					waitTimeLeftMillis = currentTimeMillis() - waitStarted;
				} catch (TimeoutException ex) {
					if (lastError != null)
						throw lastError;
					failNotCompleted(await);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				} catch (ExecutionException ex) {
					fail("await test threw unexpected exception: ", ex.getCause());
				}
			} finally {
				if (error != null)
					error.cancel(true);
			}
		}
		if (lastError != null)
			throw lastError;
		failNotCompleted(await);
	}

	private static void failNotCompleted(Await await) {
		fail(format(
				"await never completed a test execution in the maximum time %s",
				await.maxWaitTime));
	}

	private static void sleepUninterruptible(long sleepForMillis) {
		boolean interrupted = false;
		try {
			long remainingMillis = sleepForMillis;
			long end = currentTimeMillis() + remainingMillis;
			while(true) {
				try {
					TimeUnit.MILLISECONDS.sleep(remainingMillis);
					return;
				} catch (InterruptedException ex) {
					interrupted = true;
					remainingMillis = end - currentTimeMillis();
				}
			}
		} finally {
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}
}
