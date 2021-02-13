package test.integration.contract;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.contract.ContractModule;
import se.jbee.inject.contract.EventPolicy;
import se.jbee.inject.contract.EventProcessor;
import se.jbee.inject.contract.PolicyProvider;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static test.integration.util.TestUtils.wait50;

/**
 * This test illustrates the most basic scenario of a classic "event listener"
 * case where calls/messages are dispatched to multiple listeners for different
 * events (here {@link Listener#onX(int)} and {@link Listener#onY(int)}).
 *
 * While the dispatch is asynchronous the test setting uses a
 * {@link EventPolicy#maxConcurrency} of 1 which only allows one thread
 * calling any of the two methods at the same time which means the other message
 * can only be delivered after the first has been delivered successful.
 *
 * Indirectly this test also makes sure the multi-dispatch for void returning
 * methods is asynchronously processed as the event processors worker threads
 * both will be waiting in the {@link Service} methods to allow to check state
 * which will only resolve if the main thread continues in the test method to
 * the point where it notifies the waiting worker threads.
 */
class TestNonConcurrentVoidMultiDispatchEvents {

	public interface Listener {

		void onX(int x);

		void onY(int y);
	}

	public static final class Service implements Listener {

		static char nextName = 'a';

		/**
		 * Need volatile as the test thread should insect the changes
		 */
		volatile List<Integer> xs = new ArrayList<>();
		volatile List<Integer> ys = new ArrayList<>();

		/**
		 * For distinction of a and b during debugging only
		 */
		private final char name = nextName++;

		@Override
		public void onX(int x) {
			xs.add(x);
			waitHere();
		}

		@Override
		public void onY(int y) {
			ys.add(y);
			waitHere();
		}

		private void waitHere() {
			synchronized (this) {
				try {
					wait();
				} catch (InterruptedException ignored) {
				}
			}
		}

		@Override
		public String toString() {
			return name + ": " + xs.toString() + ys.toString();
		}
	}

	private static final class TestMultiDispatchEventsModule
			extends ContractModule {

		@Override
		protected void declare() {
			handle(Listener.class);
			per(Scope.injection).construct(Service.class); // dirty way to get multiple services
			// makes observed calls "single threaded"
			bind(PolicyProvider.class).to(
					event -> EventPolicy.DEFAULT.withMaxConcurrency(1));
		}

	}

	private final Injector injector = Bootstrap.injector(
			TestMultiDispatchEventsModule.class);

	@Test
	void onlyOneThreadAtATimeCallsSameListersMethods() {
		Listener listener = injector.resolve(Listener.class);
		Service a = injector.resolve(Service.class);
		Service b = injector.resolve(Service.class);
		assertNotNull(listener);
		assertNotSame(a, b);

		listener.onX(42);
		listener.onY(13);
		giveSomeTime();
		// there are two scenarios as we cannot be sure which message is first processed to the point where the onX/onY is called
		if (a.xs.isEmpty()) {
			assertMessages(a.xs);
			assertMessages(a.ys, 13);
			assertMessages(b.xs, 42);
			assertMessages(b.ys);
		} else {
			assertMessages(a.xs, 42);
			assertMessages(a.ys);
			assertMessages(b.xs);
			assertMessages(b.ys, 13);
		}
		notifyAll(a); // let a continue
		giveSomeTime();
		// no change as b is still in the handler method and thereby blocks the other event from sending it to b
		if (a.xs.isEmpty()) {
			assertMessages(a.xs);
			assertMessages(a.ys, 13);
			assertMessages(b.xs, 42);
			assertMessages(b.ys);
		} else {
			assertMessages(a.xs, 42);
			assertMessages(a.ys);
			assertMessages(b.xs);
			assertMessages(b.ys, 13);
		}
		notifyAll(b); // let b continue
		giveSomeTime();
		// now both messages are delivered to a and b
		assertMessages(a.xs, 42);
		assertMessages(a.ys, 13);
		assertMessages(b.xs, 42);
		assertMessages(b.ys, 13);
		notifyAll(a); // just complete the wait after we are done
		notifyAll(b);
		// no further event should be delivered
		assertMessages(a.xs, 42);
		assertMessages(a.ys, 13);
		assertMessages(b.xs, 42);
		assertMessages(b.ys, 13);
	}

	/**
	 * Called when the {@link EventProcessor} should be given some time to run
	 * the expected events.
	 */
	private static void giveSomeTime() {
		wait50();
	}

	private static void notifyAll(Service a) {
		synchronized (a) {
			a.notifyAll();
		}
	}

	private static void assertMessages(List<Integer> actual, int... expected) {
		assertEquals(expected.length, actual.size(),
				"Number of message not same:");
		for (int i = 0; i < expected.length; i++)
			assertEquals(expected[i], actual.get(i).intValue());
	}
}
